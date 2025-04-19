package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static tech.greenfield.vertx.irked.Matchers.isSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.status.OK;

public class TestMiddlewareEndpoint extends TestBase {

	public class TestController extends Controller {
		@Endpoint
		WebHandler middleware = r -> {
			r.put("middleware-ran", true);
			r.next();
		};
		
		@Get("/")
		WebHandler index = r -> {
			if (r.<Boolean>get("middleware-ran") == Boolean.TRUE)
				r.send(new OK());
			else
				r.send(new InternalServerError("Middleware was not called!"));
		};
	}
	
	public class TestMainController extends Controller {
		@Endpoint("/static-test")
		TestController subController1 = new TestController();

		@Endpoint("/dynamic-test/:param")
		TestController subController2 = new TestController();
	}
	
	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestMainController(), vertx, context.succeedingThenComplete());
	}
	
	@Test
	public void testMiddlewareEndpointIsCalledWithParam(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/dynamic-test/foo").send().map(res -> {
			assertThat(res, isSuccess());
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}
	
	@Test
	public void testMiddlewareEndpointIsCalledWithoutParam(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/static-test").send().map(res -> {
			assertThat(res, isSuccess());
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}
	
}
