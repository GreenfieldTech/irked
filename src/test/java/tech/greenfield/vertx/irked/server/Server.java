package tech.greenfield.vertx.irked.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.Irked;

public class Server extends AbstractVerticle {

	private Controller test;
	private HttpServer server;
	private static HttpServerOptions httpServerOptions = new HttpServerOptions();

	public Server(Controller testController) {
		test = testController;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		Future<HttpServer> async = Future.future();
		(server = vertx.createHttpServer(getHttpServerOptions())).requestHandler(
				Irked.router(vertx).configure(test).configReport())
				.listen(config().getInteger("port"), async);
		async.map((Void) null).setHandler(startFuture);
	}

	public static HttpServerOptions getHttpServerOptions() {
		return httpServerOptions;
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		server.close(stopFuture);
	}
	
}
