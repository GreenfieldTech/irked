package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testGet(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/get").send().map(res -> {
			assertThat(res, isOK());
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
			assertThat(res, isOK());
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

}
