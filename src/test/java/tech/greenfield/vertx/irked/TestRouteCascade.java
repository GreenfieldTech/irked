package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Created;
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
			r.vertx().executeBlocking(() -> data.mergeIn(r.body().asJsonObject()))
			.onComplete(f -> {
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
	
	public class TestControllerCascadeChangedStatusCode extends Controller {
		@Post("/items")
		WebHandler createItem = r -> {
			// item may be created here
			r.response(new Created());
			r.next();
		};
		
		@Post("/items")
		@Get("/items")
		WebHandler listItems = r -> {
			r.send(List.of("item 1", "item 2"));
		};
	}

	@Test
	public void testCascadingFieldHandlers(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerCascadeField(), vertx, context.succeeding(s -> executeTest(context, vertx)));
	}

	private void executeTest(VertxTestContext context, Vertx vertx) {
		int newVal = 5;
		Checkpoint async = context.checkpoint();
		getClient(vertx).put(port, "localhost", "/").send(new JsonObject().put(fieldName, newVal)).map(r -> {
			assertThat(r, isSuccess());
			assertThat(r.bodyAsJsonObject().getInteger(fieldName), equalTo(newVal));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testNotCascadingFieldHandlers(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerNotCascadedGet(), vertx, context.succeeding(s -> executeTestNoCascade(context, vertx)));
	}

	private Future<Void> executeTestNoCascade(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		return getClient(vertx).get(port, "localhost", "/foo").send().map(r -> {
			assertThat(r, isSuccess());
			assertThat(r.bodyAsString(), equalTo("ok"));
			assertThat(failedTests.get(), equalTo(0));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async))
		.mapEmpty();
	}

	
	@Test
	public void testNotCascadingFieldHandlersOnFail(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerNotCascadedOnFail(), vertx, context.succeeding(s -> executeTestNoCascadeOnFail(context, vertx)));
	}

	private Future<Void> executeTestNoCascadeOnFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		return getClient(vertx).get(port, "localhost", "/foo").send().map(r -> {
			assertThat(r, isSuccess());
			assertThat(r.bodyAsString(), equalTo("ok"));
			assertThat(failedTests.get(), equalTo(0));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async))
		.mapEmpty();
	}
	
	@Test
	public void testCascadeChangedStatusCode(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerCascadeChangedStatusCode(), vertx, context.succeeding(s -> executeTestCascadeChangedStatusCode(context, vertx)));
	}

	private Future<Void> executeTestCascadeChangedStatusCode(VertxTestContext context, Vertx vertx) {
		var client = getClient(vertx);
		return client.get(port, "localhost", "/items").send()
				.map(r -> {
					assertThat(r, isSuccess());
					assertThat(r.bodyAsString(), is(equalTo("[\"item 1\",\"item 2\"]")));
					assertThat(r.statusCode(), is(equalTo(200)));
					return null;
				})
				.compose(__ -> client.post(port, "localhost", "/items").send("foo"))
				.map(r -> {
					assertThat(r, isSuccess());
					assertThat(r.bodyAsString(), is(equalTo("[\"item 1\",\"item 2\"]")));
					assertThat(r.statusCode(), is(equalTo(201)));
					return null;
				})
				.onSuccess(__ -> log.warn("done with test"))
				.onComplete(context.succeedingThenComplete())
				.mapEmpty();
	}
}
