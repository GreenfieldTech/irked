package tech.greenfield.vertx.irked;


import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.OK;

public class TestMultipleEndpoints extends TestBase {

	public class TestControllerEndpoints extends Controller {
		@Endpoint("/red")
		@Endpoint("/blue")
		WebHandler handler = r -> {
			r.sendContent("Got URI: " + r.request().absoluteURI(), new OK());
		};
	}

	@Test
	public void testMultipleEndpoints(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerEndpoints(), vertx, context.succeeding(s -> executeTest(context, vertx)));
	}

	private void executeTest(VertxTestContext context, Vertx vertx) {
		Checkpoint asyncRed = context.checkpoint();
		Checkpoint asyncBlue = context.checkpoint();
		
		getClient(vertx).get(port, "localhost", "/red").sendP().thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, bodyContains("red"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(asyncRed::flag);
		
		getClient(vertx).get(port, "localhost", "/blue").sendP().thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, bodyContains("blue"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(asyncBlue::flag);
	}

}
