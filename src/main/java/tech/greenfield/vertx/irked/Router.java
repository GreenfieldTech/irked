package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class Router implements io.vertx.ext.web.Router {

	static Logger log = LoggerFactory.getLogger(Router.class);

	private Vertx vertx;
	private io.vertx.ext.web.Router router;

	private Set<String> routePaths = new HashSet<>();

	public Router(Vertx vertx) {
		this.vertx = vertx;
		this.router = io.vertx.ext.web.Router.router(this.vertx);
	}

	public Router with(Controller api) throws InvalidRouteConfiguration {
		return with(api, "/");
	}

	public Router with(Controller api, String path) throws InvalidRouteConfiguration {
		configure(api, path);
		return this;
	}

	public Router remove(Controller api) {
		api.remove();
		return this;
	}

	public Router configReport() {
		routePaths.stream().sorted().forEach(p -> System.out.println(p));
		return this;
	}

	public Router configure(Controller api) throws InvalidRouteConfiguration {
		return configure(api, "/");
	}

	public Router configure(Controller api, String path) throws InvalidRouteConfiguration {
		configure(api, path, new RequestWrapper(api, Request::new));
		return this;
	}

	private void configure(Controller api, String prefix, RequestWrapper requestWrapper)
			throws InvalidRouteConfiguration {
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
			tryConfigureRoute(router::get, prefix, f, WebSocket.class, requestWrapper);
		}
	}

	private <T extends Annotation> void tryConfigureRoute(RoutingMethod method, String prefix, RouteConfiguration conf,
			Class<T> anot, RequestWrapper requestWrapper) throws InvalidRouteConfiguration {
		if (conf.isController()) {
			Controller ctr = Objects.requireNonNull(conf.getController(),
					"Sub-Controller for " + conf + " is not set!");
			for (String path : conf.pathsForAnnotation(prefix, anot).collect(Collectors.toList()))
				configure(ctr, path, new RequestWrapper(ctr, requestWrapper));
			return;
		}

		conf.buildRoutesFor(prefix, anot, method, requestWrapper).forEach(routePaths::add);
	}

	/**
	 * Helper interface for
	 * {@link Router#tryConfigureRoute(RoutingMethod, Field, Class)}
	 *
	 * @author odeda
	 */
	@FunctionalInterface
	interface RoutingMethod {
		public io.vertx.ext.web.Route setRoute(String route);
	}

	public Route route() {
		return router.route();
	}

	public Route route(HttpMethod method, String path) {
		return router.route(method, path);
	}

	public Route route(String path) {
		return router.route(path);
	}

	public Route routeWithRegex(HttpMethod method, String regex) {
		return router.routeWithRegex(method, regex);
	}

	public Route routeWithRegex(String regex) {
		return router.routeWithRegex(regex);
	}

	public Route get() {
		return router.get();
	}

	public Route get(String path) {
		return router.get(path);
	}

	public Route getWithRegex(String regex) {
		return router.getWithRegex(regex);
	}

	public Route head() {
		return router.head();
	}

	public Route head(String path) {
		return router.head(path);
	}

	public Route headWithRegex(String regex) {
		return router.headWithRegex(regex);
	}

	public Route options() {
		return router.options();
	}

	public Route options(String path) {
		return router.options(path);
	}

	public Route optionsWithRegex(String regex) {
		return router.optionsWithRegex(regex);
	}

	public Route put() {
		return router.put();
	}

	public Route put(String path) {
		return router.put(path);
	}

	public Route putWithRegex(String regex) {
		return router.putWithRegex(regex);
	}

	public Route post() {
		return router.post();
	}

	public Route post(String path) {
		return router.post(path);
	}

	public Route postWithRegex(String regex) {
		return router.postWithRegex(regex);
	}

	public Route delete() {
		return router.delete();
	}

	public Route delete(String path) {
		return router.delete(path);
	}

	public Route deleteWithRegex(String regex) {
		return router.deleteWithRegex(regex);
	}

	public Route trace() {
		return router.trace();
	}

	public Route trace(String path) {
		return router.trace(path);
	}

	public Route traceWithRegex(String regex) {
		return router.traceWithRegex(regex);
	}

	public Route connect() {
		return router.connect();
	}

	public Route connect(String path) {
		return router.connect(path);
	}

	public Route connectWithRegex(String regex) {
		return router.connectWithRegex(regex);
	}

	public Route patch() {
		return router.patch();
	}

	public Route patch(String path) {
		return router.patch(path);
	}

	public Route patchWithRegex(String regex) {
		return router.patchWithRegex(regex);
	}

	public List<Route> getRoutes() {
		return router.getRoutes();
	}

	public io.vertx.ext.web.Router clear() {
		return router.clear();
	}

	public io.vertx.ext.web.Router mountSubRouter(String mountPoint, io.vertx.ext.web.Router subRouter) {
		return router.mountSubRouter(mountPoint, subRouter);
	}

	@Deprecated
	public io.vertx.ext.web.Router exceptionHandler(Handler<Throwable> exceptionHandler) {
		return router.exceptionHandler(exceptionHandler);
	}

	public void handleContext(RoutingContext context) {
		router.handleContext(context);
	}

	public void handleFailure(RoutingContext context) {
		router.handleFailure(context);
	}

	public void handle(HttpServerRequest event) {
		router.handle(event);
	}

	@Override
	public io.vertx.ext.web.Router errorHandler(int statusCode, Handler<RoutingContext> errorHandler) {
		router.errorHandler(statusCode, errorHandler);
		return this;
	}

	@Override
	public io.vertx.ext.web.Router modifiedHandler(Handler<io.vertx.ext.web.Router> handler) {
		router.modifiedHandler(handler);
		return this;
	}

	@Override
	public io.vertx.ext.web.Router allowForward(boolean allow) {
		router.allowForward(allow);
		return this;
	}

}
