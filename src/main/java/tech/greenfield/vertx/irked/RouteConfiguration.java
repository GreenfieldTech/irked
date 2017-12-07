package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.Router.RoutingMethod;
import tech.greenfield.vertx.irked.annotations.Blocking;
import tech.greenfield.vertx.irked.annotations.Consumes;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public abstract class RouteConfiguration {
	static Package annotationPackage = Endpoint.class.getPackage();

	private Annotation[] annotations;

	protected Controller impl;

	protected RouteConfiguration(Controller impl, Annotation[] annotations) {
		this.annotations = annotations;
		this.impl = impl;
	}
	
	static RouteConfiguration wrap(Controller impl, Field f) {
		return new RouteConfigurationField(impl, f);
	}
	
	static RouteConfiguration wrap(Controller impl, Method m) {
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

	boolean isBlocking() {
		return getAnnotation(Blocking.class).length > 0;
	}
	
	boolean isFailHandler() {
		return getAnnotation(OnFail.class).length > 0;
	}

	String consumes() {
		Consumes[] consumes = getAnnotation(Consumes.class);
	}
	
	public Route getRoute(RoutingMethod method, String path) {
		Route route = method.setRoute(path);
		
		return route;
	}

	
}
