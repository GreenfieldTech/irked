package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class Router {
	
	static Logger log = LoggerFactory.getLogger(Router.class);
	
	public class RequestWrapper implements Function<RoutingContext, Request>, Handler<RoutingContext> {

		private Controller ctr;
		private Function<RoutingContext, Request> wrapper;
		private Handler<? super RoutingContext> handler;

		public RequestWrapper(Controller ctr, Function<RoutingContext, Request> requestWrapper) {
			this.ctr = Objects.requireNonNull(ctr, "Controller instance is not set!");
			this.wrapper = requestWrapper;
		}

		public RequestWrapper(Handler<? super RoutingContext> handler, Function<RoutingContext, Request> requestWrapper) {
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

	public void configure(Controller api) throws InvalidRouteConfiguration {
		configure(api, "", new RequestWrapper(api, Request::new));
	}

	private void configure(Controller api, String prefix, RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		// clean up mount path
		if (prefix.endsWith("/"))
			prefix = prefix.substring(0, prefix.length() - 1);
		
		for (RouteConfiguration f : api.getRoutes()) {
			tryConfigureRoute(router::route, prefix, f, Endpoint.class, requestWrapper);
			tryConfigureRoute(router::post, prefix, f, Post.class, requestWrapper);
			tryConfigureRoute(router::get, prefix, f, Get.class, requestWrapper);
			tryConfigureRoute(router::put, prefix, f, Put.class, requestWrapper);
			tryConfigureRoute(router::delete, prefix, f, Delete.class, requestWrapper);
		}
	}

	private <T extends Annotation> boolean tryConfigureRoute(RoutingMethod method, 
			String prefix, RouteConfiguration conf, Class<T> anot, RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		String path = conf.uriForAnnotation(anot);
		if (Objects.isNull(path))
			return false;
		path = prefix + path;
		if (path.length() > 1 && path.endsWith("/"))
			path = path.substring(0, path.length() - 1); // normalize extra paths
		
		if (conf.isController()) {
			Controller ctr = Objects.requireNonNull(conf.getController(), "Sub-Controller for " + conf + " is not set!");
			configure(ctr, path, new RequestWrapper(ctr, requestWrapper));
			return true;
		}
		
		try {
			Route route = method.setRoute(path);
			Handler<RoutingContext> handler = new RequestWrapper(Objects.requireNonNull(conf.getHandler()), requestWrapper);
			if (conf.isFailHandler())
				route.failureHandler(handler);
			if (conf.isBlocking())
				route.blockingHandler(handler);
			else
				route.handler(handler);
			return true;
		} catch (NullPointerException e) {
			// ignore NPEs created by api.getHandler - it means the field is not a handler and we skip it
		} catch (IllegalArgumentException | SecurityException | IllegalAccessException e) {
			log.error("Failed to configure handler for " + path + ": " + e,e);
		}
		return false;
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
