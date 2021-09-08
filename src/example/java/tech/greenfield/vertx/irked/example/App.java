package tech.greenfield.vertx.irked.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.Router;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.helpers.Redirect;

public class App extends AbstractVerticle {

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		Router router = new Router(vertx)
				.configure(new APIv1(), "/1")
				.configure(new APIv2(), "/2")
				.configure(new Controller() {
					@Endpoint("/")
					WebHandler latest = r -> {
						throw new Redirect("/2" + r.request().uri()).uncheckedWrap(); 
					};
				});
		vertx.createHttpServer(new HttpServerOptions()).requestHandler(router::accept)
				.listen(config().getInteger("port", 8080));
	}

}
