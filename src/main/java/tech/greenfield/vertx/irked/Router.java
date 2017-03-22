package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.*;

public class Router {
	
	public class RequestWrapper implements Function<RoutingContext, Request>, Handler<RoutingContext> {

		private Controller ctr;
		private Function<RoutingContext, Request> wrapper;
		private Handler<RoutingContext> handler;

		public RequestWrapper(Controller ctr, Function<RoutingContext, Request> requestWrapper) {
			this.ctr = Objects.requireNonNull(ctr, "Controller instance is not set!");
			this.wrapper = requestWrapper;
		}

		public RequestWrapper(Handler<RoutingContext> handler, Function<RoutingContext, Request> requestWrapper) {
			this.handler = Objects.requireNonNull(handler, "Handler instance is not set!");
			this.wrapper = requestWrapper;
		}

		@Override
		public Request apply(RoutingContext r) {
			return ctr.getRequestContext(wrapper.apply(r));
		}

		@Override
		public void handle(RoutingContext r) {
			this.handler.handle(wrapper.apply(r));
		}

	}

	private Vertx vertx;
	private io.vertx.ext.web.Router router;

	Router(Vertx vertx) {
		this.vertx = vertx;
		this.router = io.vertx.ext.web.Router.router(this.vertx);
	}
	
	public void accept(HttpServerRequest request) {
		router.accept(request);
	}

	public void configure(Controller api) {
		configure(api, "", new RequestWrapper(api, Request::new));
	}

	private void configure(Controller api, String prefix, RequestWrapper requestWrapper) {
		// clean up mount path
		if (prefix.endsWith("/"))
			prefix = prefix.substring(0, prefix.length() - 1);
		
		for (Field f : api.getRoutingFields()) {
			tryConfigureRoute(api, router::route, prefix, f, Endpoint.class, requestWrapper);
			tryConfigureRoute(api, router::post, prefix, f, Post.class, requestWrapper);
			tryConfigureRoute(api, router::get, prefix, f, Get.class, requestWrapper);
			tryConfigureRoute(api, router::put, prefix, f, Put.class, requestWrapper);
			tryConfigureRoute(api, router::delete, prefix, f, Delete.class, requestWrapper);
		};
	}

	private <T extends Annotation> boolean tryConfigureRoute(Controller api, RoutingMethod method, 
			String prefix, Field field, Class<T> anot, RequestWrapper requestWrapper) {
		String path = uriFromRoutingAnnotation(field, anot);
		if (Objects.isNull(path))
			return false;
		path = prefix + path;
		if (path.length() > 1 && path.endsWith("/"))
			path = path.substring(0, path.length() - 1); // normalize extra paths
		
		if (Controller.class.isAssignableFrom(field.getType())) {
			Controller ctr = Objects.requireNonNull(api.getController(field), "Sub-Controller for " + field + " is not set!");
			configure(ctr, path, new RequestWrapper(ctr, requestWrapper));
			return true;
		}
		
		try {
			Route route = method.setRoute(path);
			Handler<RoutingContext> handler = new RequestWrapper(Objects.requireNonNull(api.getHandler(field)), requestWrapper);
			if (isFailHandler(field))
				route.failureHandler(handler);
			if (isBlocking(field))
				route.blockingHandler(handler);
			else
				route.handler(handler);
			return true;
		} catch (NullPointerException e) {
			// ignore NPEs created by api.getHandler - it means the field is not a handler and we skip it
		} catch (IllegalArgumentException | SecurityException e) {
			// failed to reflect, just don't set the route
		}
		return false;
	}

	private static boolean isBlocking(Field field) {
		return Objects.nonNull(field.getAnnotation(Blocking.class));
	}

	private static boolean isFailHandler(Field field) {
		return Objects.nonNull(field.getAnnotation(OnFail.class));
	}

	private static <T extends Annotation> String uriFromRoutingAnnotation(Field field, Class<T> anot) {
		T spec = field.getAnnotation(anot);
		if (spec == null) return null;
		try {
			// all routing annotations store the URI path in `value()`
			return spec.getClass().getMethod("value").invoke(spec).toString();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			return null; // I don't know what it failed on, but it means it doesn't fit
		} 
	}

	/**
	 * Helper interface for {@link Router#tryConfigureRoute(RoutingMethod, Field, Class)}
	 * @author odeda
	 */
	@FunctionalInterface
	interface RoutingMethod {
		public io.vertx.ext.web.Route setRoute(String route);
	}

}
