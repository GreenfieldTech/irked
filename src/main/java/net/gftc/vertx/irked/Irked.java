package net.gftc.vertx.irked;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;

public class Irked {

	private Vertx vertx;

	public Irked(Vertx vertx) {
		this.vertx = vertx;
	}

	public Handler<HttpServerRequest> setupRequestHandler(Controller api) {
		Router router = new Router(vertx);
		router.configure(api);
		return router::accept;
	}
	
}
