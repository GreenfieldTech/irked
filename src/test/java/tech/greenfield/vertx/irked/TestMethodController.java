package tech.greenfield.vertx.irked;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Delete;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.NoContent;

public class TestMethodController extends TestBase {

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

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
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

}
