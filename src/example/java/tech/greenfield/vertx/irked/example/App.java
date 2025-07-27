package tech.greenfield.vertx.irked.example;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.Irked;
import tech.greenfield.vertx.irked.Request;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.helpers.Redirect;

/**
 * Example Irked verticle
 * @author odeda
 */
public class App extends VerticleBase {

	@Override
	public Future<HttpServer> start() throws Exception {
		System.out.println("Starting Irked example app listening on port " + config().getInteger("port", 8000));
		return vertx.createHttpServer(new HttpServerOptions())
				.requestHandler(Irked.irked(vertx).router()
						.configure(new ExampleAPIv1(), "/v1")
						.configure(new ExampleAPIv2(), "/v2")
						.configure(new Controller() {
							@Endpoint("/")
							WebHandler latest = r -> {
								throw new Redirect("/v2" + r.request().uri()).unchecked();
							};
							@OnFail
							@Endpoint("/*")
							WebHandler failureHandler = Request.failureHandler();
						}))
				.listen(config().getInteger("port", 8000));
	}

}
