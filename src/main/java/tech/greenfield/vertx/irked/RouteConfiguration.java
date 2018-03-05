package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import tech.greenfield.vertx.irked.Router.RoutingMethod;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public abstract class RouteConfiguration {
	static Package annotationPackage = Endpoint.class.getPackage();

	protected Annotation[] annotations;

	protected Controller impl;

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

	Stream<String> consumes() {
		return Arrays.stream(getAnnotation(Consumes.class)).map(a -> a.value());
	}
	
	Pattern trailingSlashRemover = Pattern.compile("./$");
	public <T extends Annotation> Stream<String> pathsForAnnotation(String prefix, Class<T> anot) {
		return Arrays.stream(uriForAnnotation(anot))
				.filter(s -> Objects.nonNull(s))
				.map(s -> prefix + s)
				.map(s -> trailingSlashRemover.matcher(s).find() ? s.substring(0, s.length() - 1) : s) // normalize trailing slashes because https://github.com/vert-x3/vertx-web/issues/786 
		;
	}

	public <T extends Annotation> List<String> buildRoutesFor(String prefix, Class<T> anot, RoutingMethod method, RequestWrapper requestWrapper) throws IllegalArgumentException, InvalidRouteConfiguration {
		List<String> out = new LinkedList<>();
		for (Route r : pathsForAnnotation(prefix, anot)
				.flatMap(s -> getRoutes(method, s))
				.collect(Collectors.toList())) {
			if (anot.equals(WebSocket.class))
				r.handler(getWebSocketHandler(requestWrapper));
			else if (isFailHandler())
				r.failureHandler(getHandler(requestWrapper));
			else
				r.handler(getHandler(requestWrapper));
			out.add(r.getPath());
		}
		return out;
	}

	private Stream<Route> getRoutes(RoutingMethod method, String s) {
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
}
