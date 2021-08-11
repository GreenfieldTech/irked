package tech.greenfield.vertx.irked;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class Router implements io.vertx.ext.web.Router {

	static Logger log = LoggerFactory.getLogger(Router.class);

	private Vertx vertx;
	private io.vertx.ext.web.Router router;

	private Set<Route> routePaths = new HashSet<>(); // used for debugging only

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
		return configReport(System.err);
	}

	public Router configReport(PrintStream reportStream) {
		reportStream.println("Configured routes:");
		routePaths.stream().sorted(this::routeComparator).forEach(r -> reportStream.println(
				"  " +
				(r.methods() == null ? "*" : r.methods().stream().map(Object::toString).collect(Collectors.joining("|"))) + " " +
				r.getPath() + " -> " + listHandlers(r)));
		return this;
	}
	
	private int routeComparator(Route a, Route b) {
		String[] aPath = a.getPath().split("/"), bPath = b.getPath().split("/");
		for (int i = 0; i < Math.min(aPath.length, bPath.length); i++) {
			int c = aPath[i].compareTo(bPath[i]);
			if (c != 0) return c;
		}
		return Integer.compare(aPath.length, bPath.length);
	}
	
	@SuppressWarnings("unchecked")
	private String listHandlers(Route r) {
		try {
			Method m = r.getClass().getDeclaredMethod("state");
			m.setAccessible(true);
			Object state = m.invoke(r);
			m = state.getClass().getDeclaredMethod("getContextHandlers");
			m.setAccessible(true);
			List<Handler<RoutingContext>> ctxhandlers = (List<Handler<RoutingContext>>) m.invoke(state);
			m = state.getClass().getDeclaredMethod("getFailureHandlers");
			m.setAccessible(true);
			List<Handler<RoutingContext>> failhandlers = (List<Handler<RoutingContext>>) m.invoke(state);
			return String.join(" ",
					(ctxhandlers != null && !ctxhandlers.isEmpty()) ? ctxhandlers.stream().map(Object::toString).collect(Collectors.joining(" ")) : "",
							failhandlers != null && !failhandlers.isEmpty() ? ("@OnFail " + failhandlers.stream().map(Object::toString).collect(Collectors.joining(" "))) : "")
					.replaceAll("\\s+", " ");
		} catch (ClassCastException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Assumptions about Vert.x RouterImpl internal state have been broken. Please fix and/or stop calling configReport");
		}
	}

	public Router configure(Controller api) throws InvalidRouteConfiguration {
		return configure(api, "/");
	}

	public Router configure(Controller api, String path) throws InvalidRouteConfiguration {
		configure(api, path, new RequestWrapper(api));
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
			tryConfigureRoute(router::head, prefix, f, Head.class, requestWrapper);
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

	@Override
	public Route route() {
		return router.route();
	}

	@Override
	public Route route(HttpMethod method, String path) {
		return router.route(method, path);
	}

	@Override
	public Route route(String path) {
		return router.route(path);
	}

	@Override
	public Route routeWithRegex(HttpMethod method, String regex) {
		return router.routeWithRegex(method, regex);
	}

	@Override
	public Route routeWithRegex(String regex) {
		return router.routeWithRegex(regex);
	}

	@Override
	public Route get() {
		return router.get();
	}

	@Override
	public Route get(String path) {
		return router.get(path);
	}

	@Override
	public Route getWithRegex(String regex) {
		return router.getWithRegex(regex);
	}

	@Override
	public Route head() {
		return router.head();
	}

	@Override
	public Route head(String path) {
		return router.head(path);
	}

	@Override
	public Route headWithRegex(String regex) {
		return router.headWithRegex(regex);
	}

	@Override
	public Route options() {
		return router.options();
	}

	@Override
	public Route options(String path) {
		return router.options(path);
	}

	@Override
	public Route optionsWithRegex(String regex) {
		return router.optionsWithRegex(regex);
	}

	@Override
	public Route put() {
		return router.put();
	}

	@Override
	public Route put(String path) {
		return router.put(path);
	}

	@Override
	public Route putWithRegex(String regex) {
		return router.putWithRegex(regex);
	}

	@Override
	public Route post() {
		return router.post();
	}

	@Override
	public Route post(String path) {
		return router.post(path);
	}

	@Override
	public Route postWithRegex(String regex) {
		return router.postWithRegex(regex);
	}

	@Override
	public Route delete() {
		return router.delete();
	}

	@Override
	public Route delete(String path) {
		return router.delete(path);
	}

	@Override
	public Route deleteWithRegex(String regex) {
		return router.deleteWithRegex(regex);
	}

	@Override
	public Route trace() {
		return router.trace();
	}

	@Override
	public Route trace(String path) {
		return router.trace(path);
	}

	@Override
	public Route traceWithRegex(String regex) {
		return router.traceWithRegex(regex);
	}

	@Override
	public Route connect() {
		return router.connect();
	}

	@Override
	public Route connect(String path) {
		return router.connect(path);
	}

	@Override
	public Route connectWithRegex(String regex) {
		return router.connectWithRegex(regex);
	}

	@Override
	public Route patch() {
		return router.patch();
	}

	@Override
	public Route patch(String path) {
		return router.patch(path);
	}

	@Override
	public Route patchWithRegex(String regex) {
		return router.patchWithRegex(regex);
	}

	@Override
	public List<Route> getRoutes() {
		return router.getRoutes();
	}

	@Override
	public io.vertx.ext.web.Router clear() {
		return router.clear();
	}

	@Override
	public Route mountSubRouter(String mountPoint, io.vertx.ext.web.Router subRouter) {
		return router.mountSubRouter(mountPoint, subRouter);
	}

	@Override
	public void handleContext(RoutingContext context) {
		router.handleContext(context);
	}

	@Override
	public void handleFailure(RoutingContext context) {
		router.handleFailure(context);
	}

	@Override
	public void handle(HttpServerRequest event) {
		router.handle(event);
	}

	@Override
	public Router errorHandler(int statusCode, Handler<RoutingContext> errorHandler) {
		router.errorHandler(statusCode, errorHandler);
		return this;
	}

	@Override
	public Router modifiedHandler(Handler<io.vertx.ext.web.Router> handler) {
		router.modifiedHandler(handler);
		return this;
	}

	@Override
	public Router allowForward(AllowForwardHeaders allowForwardHeaders) {
		router.allowForward(allowForwardHeaders);
		return this;
	}

}
