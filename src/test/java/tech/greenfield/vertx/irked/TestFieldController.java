package tech.greenfield.vertx.irked;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeeding());
	}

	@Test
	public void testGet(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/get").sendP().thenAccept(res -> {
			assertThat(res, isOK());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), equalTo(Boolean.TRUE));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testPost(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).post(port, "localhost", "/post").sendP("{}").thenAccept(res -> {
			assertThat(res, is(status(new BadRequest())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("message"), equalTo(new BadRequest().getMessage()));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testPut(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).put(port, "localhost", "/put").sendP("{}").thenAccept(res -> {
			assertThat(res, isOK());
			assertThat(res.bodyAsString(), equalTo("success"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testDelete(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).delete(port, "localhost", "/delete").sendP("{}").thenAccept(res -> {
			assertThat(res, is(status(new NoContent())));
			assertThat(res.body().length(), equalTo(0));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

}
