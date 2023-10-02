package tech.greenfield.vertx.irked;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

/**
 * Helper for creating Irked routers
 * @author odeda
 */
public class Irked {

	private Vertx vertx;
	
	private Irked(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * Vert.x-styled Irked factory method.
	 * @param vertx Vert.x instance in which to generated routers
	 * @return a new Irked instance that can generate {@link Router}s and request handlers
	 */
	public static Irked irked(Vertx vertx) {
		return new Irked(vertx);
	}
	
	/**
	 * Create an HTTP request handler, that can be used in {@link HttpServer#requestHandler()},
	 * with the provided set of APIs configured
	 * @param apis set of Irked API controllers to configure routing for
	 * @return an HTTP request handler for the Vert.x HTTP server
	 * @throws InvalidRouteConfiguration in case one of the route configurations is invalid
	 */
	public Handler<HttpServerRequest> setupRequestHandler(Controller... apis) throws InvalidRouteConfiguration {
		Router router = new Router(vertx);
		for (Controller api : apis)
			router.configure(api);
		return router;
	}
	
	/**
	 * Create a new Irked router
	 * @return router that can be used to configure Irked API controllers and handle HTTP requests from Vert.x HTTP server
	 */
	public Router router() {
		return new Router(vertx);
	}
	
	/**
	 * Create a new Irked router
	 * @param vertx Vert.x instance in which to generate routers
	 * @return router that can be used to configure Irked API controllers and handle HTTP requests from Vert.x HTTP server
	 */
	public static Router router(Vertx vertx) {
		return new Router(vertx);
	}
}
