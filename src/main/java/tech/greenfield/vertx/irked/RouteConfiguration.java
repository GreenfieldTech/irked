package tech.greenfield.vertx.irked;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

/**
 * Internal implementation that handles parsing route annotations and setting up the router.
 * This is an abstract class that implements the common logic for both routing methods and routing fields.
 */
public abstract class RouteConfiguration {
	static final Package annotationPackage = Endpoint.class.getPackage();
	static final Class<Annotation>[] routeAnnotations = findRouteAnnotations();
	
	static {
		if (routeAnnotations.length == 0)
			throw new RuntimeException("Irked failed to list routing annotations in " + annotationPackage + "!");
	}
	
	protected Annotation[] annotations;
	protected Router router;
	protected Controller impl;

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected RouteConfiguration(Controller impl, Router router, Annotation[] annotations) {
		this.annotations = annotations;
		this.router = router;
		this.impl = impl;
	}

	static RouteConfiguration wrap(Controller impl, Router router, Field f) {
		return new RouteConfigurationField(impl, router, f);
	}

	static RouteConfiguration wrap(Controller impl, Router router, Method m) throws InvalidRouteConfiguration {
		return new RouteConfigurationMethod(impl, router, m);
	}

	boolean isValid() {
		return Arrays.stream(annotations).map(a -> a.annotationType().getPackage()).anyMatch(p -> p.equals(annotationPackage));
	}
	
	@SuppressWarnings("unchecked")
	protected String[] uriForAnnotations(Class<?> ...anot) {
		if (anot.length == 0)
			anot = routeAnnotations;
		ArrayList<String> uris = new ArrayList<>();
		for (var a : anot)
			uris.addAll(uriForAnnotation((Class<Annotation>) a).collect(Collectors.toList()));
		return uris.toArray(String[]::new);
	}

	<T extends Annotation> Stream<String> uriForAnnotation(Class<T> anot) {
		Annotation[] spec = getAnnotation(anot);
		if (spec.length == 0) return Stream.of();
		try {
			// all routing annotations store the URI path in `value()`
			return Arrays.stream(spec)
					.map(s -> annotationToValue(s))
					.filter(s -> Objects.nonNull(s));
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

	abstract Handler<? super Request> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration;
	abstract Handler<? super WebSocketMessage> getMessageHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration;

	private Handler<? super Request> getFailureHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		Handler<? super Request> userHandler = getHandler();
		var failSpecs = getAnnotation(OnFail.class);
		return (Request req) -> {
			int statusCode = req.statusCode();
			for (OnFail onfail : failSpecs) {
				Class<? extends Throwable> ex = onfail.exception();
				Throwable foundException = null;
				boolean statusMatchOrUnknown = onfail.status() == -1 || statusCode == onfail.status();
				boolean failureMatchOrUnknown = (Objects.equals(ex, Throwable.class) || (foundException = req.findFailure(ex)) != null);
				if (statusMatchOrUnknown && failureMatchOrUnknown) {
					if (foundException != null)
						req.setSpecificFailure(foundException);
					userHandler.handle(req);
					return;
				}
			}
			req.next(); // no match;
		};
	}
	
	boolean isBlocking() {
		return getAnnotation(Blocking.class).length > 0;
	}

	boolean isFailHandler() {
		return getAnnotation(OnFail.class).length > 0;
	}

	boolean hasConsumes() {
		return getAnnotation(Consumes.class).length > 0;
	}
	
	boolean hasOrder() {
		return getAnnotation(Order.class).length > 0;
	}

	Timeout trygetTimeout() {
		Timeout[] ts = getAnnotation(Timeout.class);
		return ts.length > 0 ? ts[0] : null;
	}

	private Stream<String> consumes() {
		return Arrays.stream(getAnnotation(Consumes.class)).map(a -> a.value());
	}
	
	private int order() {
		return Arrays.stream(getAnnotation(Order.class)).findAny().map(Order::value).orElse(0);
	}

	Pattern trailingSlashRemover = Pattern.compile("./$");

	private List<Route> routes = new ArrayList<>();

	public <T extends Annotation> Stream<String> pathsForAnnotation(String prefix, Class<T> anot) {
		return uriForAnnotation(anot)
				.filter(s -> Objects.nonNull(s))
				.map(s -> prefix + s)
				.map(s -> trailingSlashRemover.matcher(s).find() ? s.substring(0, s.length() - 1) : s) // normalize trailing slashes because https://github.com/vert-x3/vertx-web/issues/786
		;
	}

	public <T extends Annotation> List<Route> buildRoutesFor(String prefix, Class<T> anot, RoutingMethod method,
			RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		List<Route> out = new LinkedList<>();
		for (Route r : pathsForAnnotation(prefix, anot)
				.flatMap(s -> getRoutes(method, s))
				.collect(Collectors.toList())) {
			try {
				if (anot.equals(WebSocket.class))
					r.handler(getWebSocketHandler(requestWrapper));
				else if (isFailHandler())
					r.failureHandler(wrapHandler(requestWrapper, getFailureHandler()));
				else
					r.handler(wrapHandler(requestWrapper, getHandler()));
			} catch (IllegalAccessException e) {
				throw new InvalidRouteConfiguration("Illegal access error while trying to configure " + this);
			}
			routes.add(r);
			out.add(r);
		}
		return out;
	}

	private Stream<Route> getRoutes(RoutingMethod method, String s) {
		return getRoutes(method, s, true).map(r -> hasOrder() ? r.order(order()) : r);
	}

	private Stream<Route> getRoutes(RoutingMethod method, String s, boolean withTimeout) {
		if (withTimeout) {
			Timeout t = trygetTimeout();
			if (t != null)
				return getRoutes(method, s, false).map(r -> r.handler(TimeoutHandler.create(t.value())));
		}
		if (!hasConsumes())
			return Stream.of(method.getRoute(s));
		return consumes().map(c -> method.getRoute(s).consumes(c));
	}
	
	private Handler<RoutingContext> wrapHandler(RequestWrapper parent, Handler<? super Request> userHandler)
			throws IllegalArgumentException, InvalidRouteConfiguration {
		Handler<RoutingContext> handler = new RequestWrapper(Objects.requireNonNull(userHandler), parent);
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
	protected void handleUserException(Request r, Throwable cause, String invocationDescription) {
		if (r.failed()) {
			if (r.response().headWritten()) {
				log.error("Exception in user fail route '{}', after response started - ignoring", r.normalizedPath(), cause);
				if (!r.response().ended()) r.response().end(); // at least let the client have some closure
				return;
			}
			log.warn("Exception in user fail route '{}', issuing ISE!", r.normalizedPath(), cause);
			r.send(new InternalServerError());
		}
		if (HttpError.unwrap(cause) instanceof HttpError)
			r.fail(HttpError.toHttpError(cause));
		else if (invocationDescription.contains("io.vertx.ext.web") && cause instanceof IllegalStateException) {
			// a Vert.x handler detected an invalid request
			log.warn("Handler {} encountered an illegal state: {}", invocationDescription, cause.getMessage(), cause);
			r.fail(new BadRequest("Illegal state in request", cause));
		} else {
			log.error("Handler {} threw an unexpected exception", invocationDescription, cause);
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

	@SuppressWarnings("unchecked")
	private static Class<Annotation>[] findRouteAnnotations() {
		String packageName = annotationPackage.getName();
		BufferedReader reader = new BufferedReader(new InputStreamReader(Endpoint.class.getClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/") + "/annotations.list")));
		return reader.lines().map(name -> packageName + "." + name)
				.map(className -> {
					try {
						return Class.forName(className);
					} catch (ClassNotFoundException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.filter(c -> c.isAnnotation())
				.filter(c -> c.getAnnotation(RouteSpec.class) != null)
				.map(c -> (Class<Annotation>)c)
				.collect(Collectors.toSet()).toArray(Class[]::new);
	}

}
