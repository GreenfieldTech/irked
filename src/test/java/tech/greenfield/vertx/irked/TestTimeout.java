package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Timeout;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.ServiceUnavailable;

public class TestTimeout extends TestBase {

	public class TestControllerThatTimesout extends Controller {
		@Get("/toolong")
		@Timeout(5000)
		void toolong(Request r) {
			r.vertx().setTimer(TimeUnit.SECONDS.toMillis(7), res -> {
				r.sendContent("OK");
			});
		}
		
		@Get("/justintime")
		@Timeout(5000)
		void justintime(Request r) {
			r.vertx().setTimer(TimeUnit.SECONDS.toMillis(2), res -> {
				r.sendContent("OK");
			});
		}
	}

	@Test
	public void testAnOKCall(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerThatTimesout(), vertx, context.succeeding(s -> executeOKTest(context, vertx)));
	}

	private void executeOKTest(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		
		getClient(vertx).get(port, "localhost", "/justintime").send().map(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("OK"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testATimeoutCall(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerThatTimesout(), vertx, context.succeeding(s -> executeTimeoutTest(context, vertx)));
	}

	private void executeTimeoutTest(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		
		getClient(vertx).get(port, "localhost", "/toolong").send().map(r -> {
			assertThat(r, is(status(new ServiceUnavailable())));
			assertThat(r, hasBody("Service Unavailable"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
