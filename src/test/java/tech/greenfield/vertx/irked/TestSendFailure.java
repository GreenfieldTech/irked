package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class TestSendFailure extends TestBase {

	public class Response {
		@JsonProperty("result")
		public String failToGetResult() {
			throw new RuntimeException("Cannot get result");
		}
	}
	
	public class TestControllerThatCantEncode extends Controller {
		@Get("/failed-to-encode")
		WebHandler cannotSucceed = r -> Future.succeededFuture(new Response())
		.compose(r::send)
		.onFailure(r::handleFailure);
		
		@Endpoint
		@OnFail
		WebHandler failureHandler = r -> r.sendError(new InternalServerError(r.failure().getMessage()));
	}

	@Test
	public void testForFailureToEncode(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerThatCantEncode(), vertx, context.succeeding(s -> executeFailingTest(context, vertx)));
	}

	private void executeFailingTest(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		
		getClient(vertx).get(port, "localhost", "/failed-to-encode").send().map(r -> {
			assertThat(r, is(status(new InternalServerError())));
			assertThat(r, bodyContains("Cannot get result"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
