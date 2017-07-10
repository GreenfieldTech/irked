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
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.NoContent;

@RunWith(VertxUnitRunner.class)
public class TestMethodController {

	public class TestController extends Controller {
		@Get("/get")
		public void index(Request r) {
			r.sendJSON(new JsonObject().put("success", true));
		}
		
		@Post("/post")
		public void create(Request r) {
			r.sendError(new BadRequest());
		}
		
		@Put("/put")
		public void update(Request r) {
			r.response().putHeader("Content-Length", "7").write("success").end();
		}
		
		@Delete("/delete")
		public void delete(Request r) {
			r.response(new NoContent()).end();
		}
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
	public void testGet(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/get").handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.bodyHandler(body -> {
				try {
					JsonObject o = body.toJsonObject();
					context.assertEquals(Boolean.TRUE, o.getValue("success"));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end();
	}

	@Test
	public void testPost(TestContext context) {
		Async async = context.async();
		getClient().post(port, "localhost", "/post").handler(res -> {
			context.assertEquals(new BadRequest().getStatusCode(), res.statusCode(), "Incorrect response status");
			res.bodyHandler(body -> {
				try {
					JsonObject o = body.toJsonObject();
					context.assertEquals(Boolean.TRUE, o.getValue("message"));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end("{}");
	}

	@Test
	public void testPut(TestContext context) {
		Async async = context.async();
		getClient().put(port, "localhost", "/put").handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.bodyHandler(body -> {
				context.assertEquals("success", body.toString());
			});
			async.complete();
		}).end("{}");
	}

	@Test
	public void testDelete(TestContext context) {
		Async async = context.async();
		getClient().delete(port, "localhost", "/delete").handler(res -> {
			context.assertEquals(new NoContent().getStatusCode(), res.statusCode(), "Incorrect response status");
			res.bodyHandler(body -> {
				context.assertEquals(0, body.length());
			});
			async.complete();
		}).end("{}");
	}

	protected HttpClient getClient() {
		return rule.vertx().createHttpClient(new HttpClientOptions().setIdleTimeout(0));
	}

}
