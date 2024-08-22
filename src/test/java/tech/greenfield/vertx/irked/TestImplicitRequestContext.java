package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class TestImplicitRequestContext extends TestBase {

	public static class IdContext extends Request {
		private String id;

		public IdContext(Request req) {
			super(req);
			id = req.pathParam("id");
		}
		
		public String getId() {
			return id;
		}

	}
	
	public static class ExtendedIdContext extends IdContext {
		private String foo;
		
		public ExtendedIdContext(Request req) {
			super(req);
			foo = req.pathParam("foo");
		}
		
		public String getFoo() {
			return foo;
		}
	}
	
	public class NonTrivialContext extends ExtendedIdContext {

		private String preset;

		public NonTrivialContext(Request req, String preset) {
			super(req);
			this.preset = preset;
		}
		
		public String getPreset() {
			return preset;
		}
	}
	
	public static class BadContext extends Request {
		public BadContext(int outerContext) {
			super(null);
		}
		
	}
	
	public static class ThrowingContext extends Request {

		public ThrowingContext(RoutingContext outerContext) {
			super(outerContext);
			throw new IllegalStateException();
		}
		
	}
	
	public class TestController extends Controller {
		
		@Get("/good/:id")
		public void retrieve(IdContext r) {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		}

		@Get("/good/:id/:foo")
		public void checkFoo(ExtendedIdContext r) {
			r.sendJSON(new JsonObject().put("id", r.getId()).put("foo", r.getFoo()));
		};
		
		@Get("/good-with-preset/:id/:foo")
		public void retrievePreset(NonTrivialContext r) {
			r.sendJSON(new JsonObject().put("id", r.getId()).put("foo", r.getFoo())
					.put("preset", r.getPreset()));
		}

		@Get("/good-field/:id")
		Handler<IdContext> retrieveField = r -> {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		};
		
		@Get("/good-field/:id/:foo")
		Handler<ExtendedIdContext> checkFooField = r -> {
			r.sendJSON(new JsonObject().put("id", r.getId()).put("foo", r.getFoo()));
		};
		
		@Get("/good-with-preset-field/:id/:foo")
		Handler<NonTrivialContext> retrievePresetField = r -> {
			r.sendJSON(new JsonObject().put("id", r.getId()).put("foo", r.getFoo())
					.put("preset", r.getPreset()));
		};
		
		@Get("/bad/not-constructible")
		public void badHandler(BadContext ctx) {
			System.err.println("This bad handler will never be called");
		}
		
		@Get("/bad/throwing-ctor")
		public void anotherBadHandler(ThrowingContext ctx) {
			System.err.println("This another bad handler will never be called");
		}
		
		@OnFail
		@Endpoint
		WebHandler failHandler = Request.failureHandler();
		
		@SuppressWarnings("unused")
		private NonTrivialContext createNonTrivialContext(Request r) {
			return new NonTrivialContext(r, "magic");
		}
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testPassIdToMethod(VertxTestContext context, Vertx vertx) {
		log.info("Testing get ID");
		String id = "item-name";
		getClient(vertx).get(port, "localhost", "/good/" + id).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), equalTo(id));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPassFooToMethod(VertxTestContext context, Vertx vertx) {
		log.info("Testing get extended ID");
		String id = "item-name"; String foo = "bar";
		getClient(vertx).get(port, "localhost", "/good/" + id + "/" + foo).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), is(equalTo(id)));
			assertThat(o.getString("foo"), is(equalTo(foo)));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}
	
	@Test
	public void testPassWithPreset(VertxTestContext ctx, Vertx vertx) {
		log.info("Testing get with preset");
		String id = "item-name"; String foo = "baz";
		getClient(vertx).get(port, "localhost", "/good-with-preset/" + id + "/" + foo).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), is(equalTo(id)));
			assertThat(o.getString("foo"), is(equalTo(foo)));
			assertThat(o.getString("preset"), is(equalTo("magic")));
			return null;
		})
		.onComplete(ctx.succeedingThenComplete());
	}
	
	@Test
	public void testPassIdToField(VertxTestContext context, Vertx vertx) {
		log.info("Testing get ID");
		String id = "item-name";
		getClient(vertx).get(port, "localhost", "/good-field/" + id).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), equalTo(id));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPassFooToField(VertxTestContext context, Vertx vertx) {
		log.info("Testing get extended ID");
		String id = "item-name"; String foo = "bar";
		getClient(vertx).get(port, "localhost", "/good-field/" + id + "/" + foo).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), is(equalTo(id)));
			assertThat(o.getString("foo"), is(equalTo(foo)));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}
	
	@Test
	public void testPassWithPresetField(VertxTestContext ctx, Vertx vertx) {
		log.info("Testing get with preset");
		String id = "item-name"; String foo = "baz";
		getClient(vertx).get(port, "localhost", "/good-with-preset-field/" + id + "/" + foo).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), is(equalTo(id)));
			assertThat(o.getString("foo"), is(equalTo(foo)));
			assertThat(o.getString("preset"), is(equalTo("magic")));
			return null;
		})
		.onComplete(ctx.succeedingThenComplete());
	}
	
	@Test
	@Timeout(timeUnit = TimeUnit.MINUTES, value = 5)
	public void testBadNoConstructible(VertxTestContext context, Vertx vertx) {
		log.info("Testing invalid ctor");
		getClient(vertx).get(port, "localhost", "/bad/not-constructible")
		.send().map(res -> {
			assertThat(res, status(InternalServerError.class));
			var err = res.bodyAsJsonObject();
			assertThat(err.getBoolean("status"), is(false));
			assertThat(err.getString("message"), is(equalTo("Invalid request handler class tech.greenfield.vertx.irked.TestImplicitRequestContext$TestController::badHandler: routing context param class tech.greenfield.vertx.irked.TestImplicitRequestContext$BadContext is not trivially constructed from a Request instance! If you want to use non-trivially constructed programmable requests contexts, implement Controller.getRequest(Request)")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testBadThrowingCtor(VertxTestContext context, Vertx vertx) {
		log.info("Testing throwing ctor");
		getClient(vertx).get(port, "localhost", "/bad/throwing-ctor")
		.send().map(res -> {
			assertThat(res, status(InternalServerError.class));
			var err = res.bodyAsJsonObject();
			assertThat(err.getBoolean("status"), is(false));
			assertThat(err.getString("message"), is(equalTo("Failed to construct routing context param for class tech.greenfield.vertx.irked.TestImplicitRequestContext$TestController::anotherBadHandler from Request instance")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

}
