package tech.greenfield.vertx.irked;

import org.junit.Test;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.OK;

public class TestRepeatables extends TestBase {

	public class TestRepeatableField extends Controller {
		@Post("/red")
		@Post("/blue")
		WebHandler handler = r -> {
			r.sendContent("Got URI: " + r.request().absoluteURI(), new OK());
		};
	}

	@Test
	public void testRepeatableField(TestContext context) {
		deployController(new TestRepeatableField(), context.asyncAssertSuccess(s -> executeTest(context)));
	}

	private void executeTest(TestContext context) {
		Async asyncRed = context.async();
		Async asyncBlue = context.async();
		
		getClient().post(port, "localhost", "/red").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call red");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertTrue(body.toString().contains("red"));
				asyncRed.complete();
			});
		}).end();
		
		getClient().post(port, "localhost", "/blue").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call blue");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertTrue(body.toString().contains("blue"));
				asyncBlue.complete();
			});
		}).end();
	}

}
