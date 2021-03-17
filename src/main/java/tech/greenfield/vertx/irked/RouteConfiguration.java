package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import tech.greenfield.vertx.irked.HttpError.UncheckedHttpError;
import tech.greenfield.vertx.irked.Router.RoutingMethod;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public abstract class RouteConfiguration {
	static Package annotationPackage = Endpoint.class.getPackage();

	protected Annotation[] annotations;

	protected Controller impl;

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected RouteConfiguration(Controller impl, Annotation[] annotations) {
		this.annotations = annotations;
		this.impl = impl;
	}

	static RouteConfiguration wrap(Controller impl, Field f) {
		return new RouteConfigurationField(impl, f);
	}

	static RouteConfiguration wrap(Controller impl, Method m) throws InvalidRouteConfiguration {
		return new RouteConfigurationMethod(impl, m);
	}

	boolean isValid() {
		return Arrays.stream(annotations).map(a -> a.annotationType().getPackage()).anyMatch(p -> p.equals(annotationPackage));
	}

	<T extends Annotation> String[] uriForAnnotation(Class<T> anot) {
		Annotation[] spec = getAnnotation(anot);
		if (spec.length == 0) return new String[] {};
		try {
			// all routing annotations store the URI path in `value()`
			return Arrays.stream(spec)
					.map(s -> annotationToValue(s))
					.filter(s -> Objects.nonNull(s))
					.toArray(size -> { return new String[size]; });
		} catch (RuntimeException e) {
			return null; // I don't know what it failed on, but it means it doesn't fit
		}
	}

	private String annotationToValue(Annotation anot) {
		try {
			return anot.getClass().getMethod("value").invoke(anot).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	abstract protected <T extends Annotation> T[] getAnnotation(Class<T> anot);

	abstract boolean isController();

	abstract Controller getController();

	@Override
	public String toString() {
		return impl.getClass() + "::" + getName();
	}

	abstract protected String getName();

	abstract Handler<? super RoutingContext> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration;
	abstract Handler<? super WebSocketMessage> getMessageHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration;

	boolean isBlocking() {
		return getAnnotation(Blocking.class).length > 0;
	}

	boolean isFailHandler() {
		return getAnnotation(OnFail.class).length > 0;
	}

	boolean hasConsumes() {
		return getAnnotation(Consumes.class).length > 0;
	}

	Timeout trygetTimeout() {
		Timeout[] ts = getAnnotation(Timeout.class);
		return ts.length > 0 ? ts[0] : null;
	}

	Stream<String> consumes() {
		return Arrays.stream(getAnnotation(Consumes.class)).map(a -> a.value());
	}

	Pattern trailingSlashRemover = Pattern.compile("./$");

	private List<Route> routes = new ArrayList<>();

	public <T extends Annotation> Stream<String> pathsForAnnotation(String prefix, Class<T> anot) {
		return Arrays.stream(uriForAnnotation(anot))
				.filter(s -> Objects.nonNull(s))
				.map(s -> prefix + s)
				.map(s -> trailingSlashRemover.matcher(s).find() ? s.substring(0, s.length() - 1) : s) // normalize trailing slashes because https://github.com/vert-x3/vertx-web/issues/786
		;
	}

	public <T extends Annotation> List<Route> buildRoutesFor(String prefix, Class<T> anot, RoutingMethod method, RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		List<Route> out = new LinkedList<>();
		for (Route r : pathsForAnnotation(prefix, anot)
				.flatMap(s -> getRoutes(method, s))
				.collect(Collectors.toList())) {
			if (anot.equals(WebSocket.class))
				r.handler(getWebSocketHandler(requestWrapper));
			else if (isFailHandler())
				r.failureHandler(getHandler(requestWrapper));
			else
				r.handler(getHandler(requestWrapper));
			routes.add(r);
			out.add(r);
		}
		return out;
	}

	private Stream<Route> getRoutes(RoutingMethod method, String s) {
		return getRoutes(method, s, true);
	}

	private Stream<Route> getRoutes(RoutingMethod method, String s, boolean withTimeout) {
		if (withTimeout) {
			Timeout t = trygetTimeout();
			if (Objects.nonNull(t))
				return getRoutes(method, s, false).map(r -> r.handler(TimeoutHandler.create(t.value())));
		}
		if (!hasConsumes())
			return Stream.of(method.setRoute(s));
		return consumes().map(c -> method.setRoute(s).consumes(c));
	}

	private Handler<RoutingContext> getHandler(RequestWrapper parent) throws IllegalArgumentException, InvalidRouteConfiguration {
		Handler<RoutingContext> handler;
		try {
			handler = new RequestWrapper(Objects.requireNonNull(getHandler()), parent);
		} catch (IllegalAccessException e) {
			throw new InvalidRouteConfiguration("Illegal access error while trying to configure " + this);
		}
		if (isBlocking())
			handler = new BlockingHandlerDecorator(handler, true);
		return handler;
	}

	private Handler<RoutingContext> getWebSocketHandler(RequestWrapper parent) throws IllegalArgumentException, InvalidRouteConfiguration {
		try {
			return new WebSocketUpgradeRequestWrapper(Objects.requireNonNull(getMessageHandler()), parent);
		} catch (IllegalAccessException e) {
			throw new InvalidRouteConfiguration("Illegal access error while trying to configure " + this);
		}
	}

	void remove() {
		routes.forEach(Route::remove);
	}

	/**
	 * Handling logic for user exceptions thrown from a handler invocation.
	 * Handler developers can throw an Irked {@link HttpError} exception to propagate an HTTP response
	 * to the failure handler, or an unexpected exception can be thrown which would mean a server error.
	 * Because {@link HttpError}s may be wrapped by all kinds of wrappers, from Irked {@link UncheckedHttpError} to
	 * Jackson's JsonMappingException, we try hard to extract {@link HttpError}s wherever we find them - to make
	 * the fail handler's developer's life easier.
	 *
	 * @param r routing context on which this handler was called
	 * @param cause User exception thrown from the handler
	 * @param invocationDescription Description of the invocation endpoint for logging
	 */
	protected void handleUserException(RoutingContext r, Throwable cause, String invocationDescription) {
		if (r.failed()) {
			log.warn("Exception occured on a fail route, ignoring",cause);
			return;
		}
		if (cause instanceof UncheckedHttpError || cause instanceof HttpError || HttpError.unwrap(cause) instanceof HttpError)
			r.fail(HttpError.toHttpError(cause));
		else {
			log.error("Handler " + invocationDescription + " threw an unexpected exception",cause);
			r.fail(cause); // propagate exceptions thrown by the method to the Vert.x fail handler
		}
	}

	/**
	 * Handling logic for user exceptions thrown from a message handler invocation.
	 * Note that WebSocket does not provide robust error reporting mechanisms - unless the application handles
	 * their own error detection and reporting using an application-level protocol, the only think we can do
	 * is send a close frame with a status code
	 * (<a href="https://www.iana.org/assignments/websocket/websocket.xml#close-code-number">specified by IANA</a>
	 * but almost completely useless) and a text message.
	 * 
	 * As such we expect the application developer to implement their own application level error handling protocol
	 * and here we implement a "last refuge" handler that closes the socket and attempts to provide useful information
	 * while limiting internal implementation details leakage. We assume that this method will be called in two cases:
	 * <ul>
	 * <li>The developer threw an {@link HttpError} from a message handler - which they shouldn't do, but we can try
	 * to give them a simple expected behavior by closing the socket with a 1002 ("Protocol Error") status and the
	 * message from the exception (it could be the HTTP status message, or a custom message).</li>
	 * <li>An unexpected exception was thrown from a message handler - that the developer should have caught but didn't.
	 * In that case we will close the socket with a 1011 ("Internal Error") status and the exception message. We will also
	 * dump the full exception stack to the error log.</li>
	 * </ul>
	 *
	 * @param m message that caused the exception
	 * @param cause User exception thrown from the handler
	 * @param invocationDescription Description of the invocation endpoint for logging
	 */
	protected void handleUserException(WebSocketMessage m, Throwable cause, String invocationDescription) {
		var err = HttpError.unwrap(cause);
		if (err instanceof HttpError)
			m.socket().close((short)1002, ((HttpError)err).getMessage());
		else {
			log.error("Handler " + invocationDescription + " threw an unexpected exception",cause);
			m.socket().close((short)1011, cause.getMessage());
		}
	}

}
