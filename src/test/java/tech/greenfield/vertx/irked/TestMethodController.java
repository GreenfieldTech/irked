package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Delete;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.NoContent;
import tech.greenfield.vertx.irked.status.NotFound;

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
			r.response().putHeader("Content-Length", "7").write("success");
		}
		
		@Delete("/delete")
		public void delete(Request r) {
			r.response(new NoContent()).end();
		}
		
		@Get("/get/pojo")
		public ResultPojo retrievePojo(Request r) {
			return new ResultPojo() {{ foo = "hello"; bar = 5; }};
		}
		
		@Get("/get/async-pojo")
		public Future<ResultPojo> retrieveAsyncPojo(Request r) {
			return Future.succeededFuture(new ResultPojo() {{ foo = "yellow"; bar = 7; }});
		}
		
		@Get("/get/async-pojo-self-send")
		public Future<ResultPojo> retrieveAsyncPojoButSend(Request r) {
			ResultPojo res = new ResultPojo() {{ foo = "yellow"; bar = 7; }};
			return Future.succeededFuture(res).onComplete(r::sendOrFail);
		}
		
		@Get("/get/not-found")
		public Future<ResultPojo> noSuchResult(Request r) {
			return Future.failedFuture(new NotFound("No such object"));
		}
		
		@Get("/get/not-found2")
		public ResultPojo lazyNoSuchResult(Request r) {
			return null;
		}
		
		@OnFail
		@Get("/get/not-found")
		WebHandler failureHandler = Request.failureHandler();
	}
	
	public static class ResultPojo {
		public String foo;
		public int bar;
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testGet(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/get").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), equalTo(Boolean.TRUE));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testPost(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).post(port, "localhost", "/post").send("{}").map(res -> {
			assertThat(res, is(status(new BadRequest())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("message"), equalTo(new BadRequest().getMessage()));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testPut(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).put(port, "localhost", "/put").send("{}").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res, hasBody("success"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testDelete(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).delete(port, "localhost", "/delete").send("{}").map(res -> {
			assertThat(res, is(status(new NoContent())));
			assertThat(res, is(bodyEmpty()));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testReturningMethod(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/get/pojo").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("foo"), equalTo("hello"));
			assertThat(o.getValue("bar"), equalTo(5));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testReturningAsyncMethod(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/get/async-pojo").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("foo"), equalTo("yellow"));
			assertThat(o.getValue("bar"), equalTo(7));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testReturningAsyncMethodNoDoubleSend(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/get/async-pojo-self-send").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("foo"), equalTo("yellow"));
			assertThat(o.getValue("bar"), equalTo(7));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testReturningAsyncMethodWithError(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/get/not-found").send().map(res -> {
			assertThat(res, is(notFound()));
			var result = res.bodyAsJsonObject();
			assertThat(result.getValue("message"), equalTo("No such object"));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testReturningAsyncMethodWithNull(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/get/not-found2").send().map(res -> {
			assertThat(res, isSuccess());
			var result = res.bodyAsJsonObject();
			assertThat(result, is(nullValue()));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}
}
