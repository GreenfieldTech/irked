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

	public Server(Controller testController) {
		test = testController;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		Future<HttpServer> async = Future.future();
		(server = vertx.createHttpServer(new HttpServerOptions())).requestHandler(new Irked(vertx).setupRequestHandler(test))
				.listen(config().getInteger("port"), async);
		async.map((Void) null).setHandler(startFuture);
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		server.close(stopFuture);
	}
	
}
