package tech.greenfield.vertx.irked;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.server.Server;

@RunWith(VertxUnitRunner.class)
public class TestBadController {

	public class TestControllerBadField extends Controller {
		@Get("/")
		String invalidHandler = "test";
	}

	public class TestControllerBadMethod extends Controller {
		@Get("/")
		boolean invalidMethod() {
			return true;
		}
	}

	@ClassRule
	public static RunTestOnContext rule = new RunTestOnContext();

	@Rule
	public Timeout timeoutRule = Timeout.seconds(3600);

	final Integer port = 1234;

	@Test
	public void testInvalidFieldHandlerError(TestContext context) {
		Server server = new Server(new TestControllerBadField());

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		rule.vertx().deployVerticle(server, options, context.asyncAssertFailure());
	}

	@Test
	public void testInvalidMethodHandlerError(TestContext context) {
		Server server = new Server(new TestControllerBadMethod());

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		rule.vertx().deployVerticle(server, options, context.asyncAssertFailure());
	}

}
