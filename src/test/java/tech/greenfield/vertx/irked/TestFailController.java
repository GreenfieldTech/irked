package tech.greenfield.vertx.irked;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
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

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeeding());
	}

	@Test
	public void testFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/").sendP().thenAccept(res -> {
			assertThat(res, isOK());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), equalTo(Boolean.FALSE));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

}
