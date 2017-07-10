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
public class TestFailController {

	public class TestController extends Controller {
		@OnFail
		@Endpoint("/*")
		WebHandler failureHandler = r -> {
			r.sendJSON(new JsonObject().put("success", false));
		};
		
		@Get("/")
		WebHandler index = r -> {
			throw new RuntimeException();
		};
	}

	@ClassRule
	public static RunTestOnContext rule = new RunTestOnContext();

	@Rule
	public Timeout timeoutRule = Timeout.seconds(3600);

	final Integer port = 1234;

	@Before
	public void deployServer(TestContext context) {
		Server server = new Server(new TestController());

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		rule.vertx().deployVerticle(server, options, context.asyncAssertSuccess());
	}

	@Test
	public void testFail(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/").handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.bodyHandler(body -> {
				try {
					JsonObject o = body.toJsonObject();
					context.assertEquals(Boolean.FALSE, o.getValue("success"));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end();
	}

	protected HttpClient getClient() {
		return rule.vertx().createHttpClient(new HttpClientOptions().setIdleTimeout(0));
	}

}
