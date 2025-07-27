package tech.greenfield.vertx.irked.server;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.Irked;

public class Server extends VerticleBase {

	private Controller test;
	private HttpServer server;
	private static HttpServerOptions httpServerOptions = new HttpServerOptions();

	public Server(Controller testController) {
		test = testController;
	}

	@Override
	public Future<HttpServer> start() throws Exception {
		return (server = vertx.createHttpServer(getHttpServerOptions())).requestHandler(
				Irked.router(vertx).configure(test).configReport(System.out))
				.listen(config().getInteger("port"));
	}

	public static HttpServerOptions getHttpServerOptions() {
		return httpServerOptions;
	}
	
	@Override
	public Future<Void> stop() throws Exception {
		return server.close();
	}

}
