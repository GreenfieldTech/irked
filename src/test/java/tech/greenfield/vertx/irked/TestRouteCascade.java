package tech.greenfield.vertx.irked;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.handler.BodyHandler;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class TestRouteCascade extends TestBase {
	
	AtomicInteger failedTests = new AtomicInteger(0);

	String fieldName = "value";

	public class TestControllerCascadeField extends Controller {
		JsonObject data = new JsonObject().put(fieldName, 1);
		
		@Endpoint("/*")
		BodyHandler bodyHandler = BodyHandler.create();

		@Put("/")
		WebHandler update = r -> {
			rule.vertx().executeBlocking(f -> {
				data.mergeIn(r.getBodyAsJson());
				f.complete();
			}, f -> {
				if (f.failed())
					r.sendError(new InternalServerError(f.cause()));
				else
					r.next();
			});
		};

		@Put("/")
		@Get("/")
		WebHandler retrieve = r -> {
			r.sendJSON(data);
		};
	}
	
	public class TestControllerNotCascadedGet extends Controller {
		
		@Get("/foo")
		WebHandler foo1 = r -> {
			r.send("ok");
		};

		@Get("/foo")
		WebHandler foo2 = r -> {
			failedTests.incrementAndGet();
			r.send("shouldn't happen");
		};

	}
	
	public class TestControllerNotCascadedOnFail extends Controller {
		
		@Get("/foo")
		WebHandler foo1 = r -> {
			r.fail(new Exception("first route fails"));
		};

		@Get("/foo")
		WebHandler foo2 = r -> {
			System.err.println("Not being called");
			failedTests.incrementAndGet();
			r.send("shouldn't happen");
			throw new Error();
		};
		
		@OnFail
		@Get("/foo")
		WebHandler fooFailed = r -> {
			if (r.failure().getMessage().equals("first route fails"))
				r.send("ok");
			else
				r.sendError(new InternalServerError());
		};

	}

	@Test
	public void testCascadingFieldHandlers(TestContext context) {
		deployController(new TestControllerCascadeField(), context.asyncAssertSuccess(s -> executeTest(context)));
	}

	private void executeTest(TestContext context) {
		int newVal = 5;
		Async async = context.async();
		getClient().put(port, "localhost", "/").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call PUT");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertEquals(newVal, body.toJsonObject().getInteger(fieldName));
				async.complete();
			});
		}).end(new JsonObject().put(fieldName, newVal).encode());
	}

	@Test
	public void testNotCascadingFieldHandlers(TestContext context) {
		deployController(new TestControllerNotCascadedGet(), context.asyncAssertSuccess(s -> executeTestNoCascade(context)));
	}

	private void executeTestNoCascade(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/foo").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call GET /foo");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertEquals("ok", body.toString());
				context.assertEquals(0, failedTests.get());
				async.complete();
			});
		}).end();
	}

	
	@Test
	public void testNotCascadingFieldHandlersOnFail(TestContext context) {
		deployController(new TestControllerNotCascadedOnFail(), context.asyncAssertSuccess(s -> executeTestNoCascadeOnFail(context)));
	}

	private void executeTestNoCascadeOnFail(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/foo").exceptionHandler(t -> context.fail(t)).handler(r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call GET /foo");
			r.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				context.assertEquals("ok", body.toString());
				context.assertEquals(0, failedTests.get());
				async.complete();
			});
		}).end();
	}

	
}
