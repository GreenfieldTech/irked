package tech.greenfield.vertx.irked;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Timeout;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestTimeout extends TestBase {

	public class TestControllerThatTimesout extends Controller {
		@Get("/toolong")
		@Timeout(5000)
		void toolong(Request r) {
			rule.vertx().setTimer(TimeUnit.SECONDS.toMillis(7), res -> {
				r.sendContent("OK");
			});
		}
		
		@Get("/justintime")
		@Timeout(5000)
		void justintime(Request r) {
			rule.vertx().setTimer(TimeUnit.SECONDS.toMillis(2), res -> {
				r.sendContent("OK");
			});
		}
	}

	@Test
	public void testAnOKCall(TestContext context) {
		deployController(new TestControllerThatTimesout(), context.asyncAssertSuccess(s -> executeOKTest(context)));
	}

	private void executeOKTest(TestContext context) {
		Async async = context.async();
		
		getClient().get(port, "localhost", "/justintime").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call justintime");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertEquals("OK", body.toString());
				async.complete();
			});
		}).end();
	}

	@Test
	public void testATimeoutCall(TestContext context) {
		deployController(new TestControllerThatTimesout(), context.asyncAssertSuccess(s -> executeTimeoutTest(context)));
	}

	private void executeTimeoutTest(TestContext context) {
		Async async = context.async();
		
		getClient().get(port, "localhost", "/toolong").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(503, r.statusCode(), "Failed to call toolong");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertEquals("Service Unavailable", body.toString());
				async.complete();
			});
		}).end();
	}

}
