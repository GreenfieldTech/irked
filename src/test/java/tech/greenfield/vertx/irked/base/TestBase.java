package tech.greenfield.vertx.irked.base;

import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.core.*;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.server.Server;

public class TestBase {

	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024;

	@RegisterExtension
	static VertxExtension vertxExtension = new VertxExtension();
	protected final Integer port = new Random().nextInt(30000)+10000;

	protected WebClientExt getClient(Vertx vertx) {
		return new WebClientExt(vertx, new WebClientOptions(new HttpClientOptions()
				.setIdleTimeout(0)
				.setMaxWebsocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE)));
	}

	protected void deployController(Controller controller, Vertx vertx, Handler<AsyncResult<String>> handler) {
		Server server = new Server(controller);

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		vertx.deployVerticle(server, options, handler);
	}

	protected Function<Throwable, Void> failureHandler(VertxTestContext context) {
		return  t -> {
			context.failNow(t);
			return null;
		};
	}

}
