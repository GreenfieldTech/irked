package tech.greenfield.vertx.irked;

import static tech.greenfield.vertx.irked.Matchers.*;
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
		
		getClient(vertx).get(port, "localhost", "/red").send().map(r -> {
			assertThat(r, isOK());
			assertThat(r, bodyContains("red"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(asyncRed));
		
		getClient(vertx).get(port, "localhost", "/blue").send().map(r -> {
			assertThat(r, isOK());
			assertThat(r, bodyContains("blue"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(asyncBlue));
	}

}
