package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Order;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestExplicitOrder extends TestBase {

	public class TestController extends Controller {
		
		@Get()
		@Order(3)
		public void handlerA(Request r) {
			r.put("result", r.get("result", "") + "a");
			r.next();
		}

		@Get()
		@Order(-1)
		public void handlerB(Request r) {
			r.put("result", r.get("result", "") + "b");
			r.next();
		}

		@Get()
		@Order(0)
		public void handlerC(Request r) {
			r.put("result", r.get("result", "") + "c");
			r.next();
		}

		@Get()
		@Order(1000)
		public void output(Request r) {
			r.send(r.get("result", ""));
		}
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testOrderedComposition(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), equalTo("bca"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
