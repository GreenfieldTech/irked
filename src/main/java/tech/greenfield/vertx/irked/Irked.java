package tech.greenfield.vertx.irked;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class Irked {

	private Vertx vertx;

	public Irked(Vertx vertx) {
		this.vertx = vertx;
	}

	public Handler<HttpServerRequest> setupRequestHandler(Controller... apis) throws InvalidRouteConfiguration {
		Router router = new Router(vertx);
		for (Controller api : apis)
			router.configure(api);
		return router;
	}
	
	public static Router router(Vertx vertx) {
		return new Router(vertx);
	}
}
