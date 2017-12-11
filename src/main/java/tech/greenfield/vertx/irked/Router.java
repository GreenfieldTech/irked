package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class Router {
	
	static Logger log = LoggerFactory.getLogger(Router.class);
	
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
			tryConfigureRoute(router::patch, prefix, f, Patch.class, requestWrapper);
			tryConfigureRoute(router::options, prefix, f, Options.class, requestWrapper);
		}
	}

	private <T extends Annotation> void tryConfigureRoute(RoutingMethod method, 
			String prefix, RouteConfiguration conf, Class<T> anot, RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		if (conf.isController()) {
			Controller ctr = Objects.requireNonNull(conf.getController(), "Sub-Controller for " + conf + " is not set!");
			for (String path : conf.pathsForAnnotation(prefix, anot).collect(Collectors.toList()))
				configure(ctr, path, new RequestWrapper(ctr, requestWrapper));
			return;
		}
		
		conf.buildRoutesFor(prefix, anot, method, requestWrapper);
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
