package tech.greenfield.vertx.irked;

import java.util.Objects;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.server.Server;

@RunWith(VertxUnitRunner.class)
public class TestRequestContext {

	public class IdContext extends Request {

		private String id;

		public IdContext(Request req, String currentId) {
			super(req);
			id = currentId;
		}
		
		public String getId() {
			return id;
		}

	}

	public class TestController extends Controller {
		
		@Get("/:id")
		public void retrieve(IdContext r) {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		}

		@Put("/:id")
		Handler<IdContext> update = r -> {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		};

		@Override
		protected Request getRequestContext(Request req) {
			String currentId = req.pathParam("id");
			if (Objects.isNull(currentId))
				return req;
			return new IdContext(req, currentId);
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
	public void testPassIdToMethod(TestContext context) {
		Async async = context.async();
		String id = "item-name";
		getClient().get(port, "localhost", "/" + id).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.bodyHandler(body -> {
				try {
					JsonObject o = body.toJsonObject();
					context.assertEquals(id, o.getString("id"));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end();
	}


	@Test
	public void testPassIdToField(TestContext context) {
		Async async = context.async();
		String id = "item-name";
		getClient().put(port, "localhost", "/" + id).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.bodyHandler(body -> {
				try {
					JsonObject o = body.toJsonObject();
					context.assertEquals(id, o.getString("id"));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end("{}");
	}

	protected HttpClient getClient() {
		return rule.vertx().createHttpClient(new HttpClientOptions().setIdleTimeout(0));
	}

}
