package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.annotations.Order;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.status.Locked;

public class TestExceptionFailWithParams extends TestBase {
	
	public class FancyException extends Exception {
		private static final long serialVersionUID = 1L;
		int number;
		public FancyException(int number) {
			this.number = number;
		}
		public int getNumber() {
			return number;
		}
	}

	public class TestController extends Controller {
		@Post("/test-ok-error")
		@Order(0)
		public void callFailException(Request r) {
			r.fail(new FancyException(77));
		}
		
		@OnFail(exception = FancyException.class)
		@Post("/test-ok-error")
		@Order(1)
		public void handleFailException(Request r, FancyException error) {
			r.sendContent(String.valueOf(error.getNumber()), new Locked());
		}
		
		@OnFail
		@Endpoint
		@Order(1000)
		WebHandler defaultFailHandler = r -> {
			r.sendJSON(new JsonObject().put("did-default", true),
					new InternalServerError());
		};
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testFailureUsingMethods(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).post(port, "localhost", "/test-ok-error").send("boo!").map(res -> {
			System.out.println("response: " + res.bodyAsString());
			assertThat(res, is(status(new Locked())));
			var body = res.body().toString();
			assertThat(body, is(equalTo("77")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
