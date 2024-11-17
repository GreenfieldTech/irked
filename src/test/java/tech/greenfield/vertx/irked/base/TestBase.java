package tech.greenfield.vertx.irked.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.notFound;

import java.util.Random;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.server.Server;

public class TestBase {
	
	protected Logger log = LoggerFactory.getLogger(getClass());

	// client max message
	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024; // 1G. Frame size is 64K

	static {
		System.setProperty("vertx.parameter.filename", "vertx-test-options.json");
	}
	@RegisterExtension
	private static VertxExtension vertxExtension = new VertxExtension();
	protected final Integer port = new Random().nextInt(30000)+20000;

	protected static WebClientExt getClient(Vertx vertx) {
		return new WebClientExt(vertx, new WebClientOptions(new HttpClientOptions()
				.setIdleTimeout(0)
				.setMaxChunkSize(MAX_WEBSOCKET_MESSAGE_SIZE)));
	}
	
	protected void deployController(Controller controller, Vertx vertx, Handler<AsyncResult<String>> handler) {
		Server server = new Server(controller);

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		vertx.deployVerticle(server, options).andThen(handler);
	}

	protected void verifyNotFound(HttpResponse<Buffer> r) {
		assertThat(r, is(notFound()));
	}
	
	protected static <G> Handler<G> flag(Checkpoint cp) {
		return g -> { cp.flag(); };
	}

}
