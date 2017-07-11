package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.Blocking;
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
	
	<T extends Annotation> String uriForAnnotation(Class<T> anot) {
		Annotation spec = getAnnotation(anot);
		if (spec == null) return null;
		try {
			// all routing annotations store the URI path in `value()`
			return spec.getClass().getMethod("value").invoke(spec).toString();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			return null; // I don't know what it failed on, but it means it doesn't fit
		} 
	}

	abstract protected <T extends Annotation> T getAnnotation(Class<T> anot);

	abstract boolean isController();

	abstract Controller getController();

	@Override
	public String toString() {
		return impl.getClass() + "::" + getName();
	}

	abstract protected String getName();

	abstract Handler<? super RoutingContext> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration;

	boolean isBlocking() {
		return Objects.nonNull(getAnnotation(Blocking.class));
	}

	boolean isFailHandler() {
		return Objects.nonNull(getAnnotation(OnFail.class));
	}

	
}
