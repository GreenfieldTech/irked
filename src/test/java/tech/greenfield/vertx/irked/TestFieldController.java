package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Delete;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.NoContent;
import tech.greenfield.vertx.irked.status.OK;
import tech.greenfield.vertx.irked.status.Unauthorized;

public class TestFieldController extends TestBase {

	public class TestController extends Controller {
		@Get("/get")
		WebHandler index = r -> {
			r.sendJSON(new JsonObject().put("success", true));
		};
		
		@Post("/post")
		WebHandler create = r -> {
			r.sendError(new BadRequest());
		};
		
		@Put("/put")
		WebHandler update = r -> {
			r.response().putHeader("Content-Length", "7").write("success");
		};
		
		@Delete("/delete")
		WebHandler delete = r -> {
			r.response(new NoContent()).end();
		};
		
		class Result {
			public String message;
		}
		
		@Post("/post-function")
		WebResult<Request> createFunc = r -> new Result() {{ message = "created"; }};
		
		@Delete("/delete-function")
		SimpleWebResult deleteFunc = r -> Future.failedFuture(new Unauthorized());
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
			assertThat(res.bodyAsString(), equalTo("success"));
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
			assertThat(res.body(), is(nullValue()));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testPostFunction(VertxTestContext context, Vertx vertx) {
		getClient(vertx).post(port, "localhost", "/post-function").send("{}").map(res -> {
			assertThat(res, is(status(new OK())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("message"), equalTo("created"));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testDeleteFunction(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).delete(port, "localhost", "/delete-function").send().map(res -> {
			assertThat(res, is(status(new Unauthorized())));
			assertThat(res.bodyAsString(), is(equalTo("Unauthorized")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}


}
