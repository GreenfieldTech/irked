package tech.greenfield.vertx.irked;

import java.util.Objects;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestRequestContext extends TestBase {

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

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
	}

	@Test
	public void testPassIdToMethod(TestContext context) {
		Async async = context.async();
		String id = "item-name";
		getClient().get(port, "localhost", "/" + id).exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
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
		getClient().put(port, "localhost", "/" + id).exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
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

}
