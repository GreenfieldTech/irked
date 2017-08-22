package tech.greenfield.vertx.irked;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestFailController extends TestBase {

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

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
	}

	@Test
	public void testFail(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
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

}
