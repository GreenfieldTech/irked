package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class TestBadController extends TestBase {

	public class TestControllerBadField extends Controller {
		@Get("/")
		String invalidHandler = "test";
	}
	
	public class TestControllerBadMethod extends Controller {
		@Get("/")
		boolean invalidMethod() {
			return true;
		}
	}
	
	@Test
	public void testInvalidFieldHandlerError(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerBadField(), vertx, context.failing(t -> {
			assertThat(t, is(instanceOf(InvalidRouteConfiguration.class)));
			assertThat(t.getMessage(), containsString("invalidHandler"));
			assertThat(t.getMessage(), containsString("not a valid handler or controller"));
			context.completeNow();
		}));
	}
	
	@Test
	public void testInvalidMethodHandlerError(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerBadMethod(), vertx, context.failing(t -> {
			assertThat(t, is(instanceOf(InvalidRouteConfiguration.class)));
			assertThat(t.getMessage(), containsString("invalidMethod"));
			assertThat(t.getMessage(), containsString("RoutingContext as first parameter"));
			context.completeNow();
		}));
	}

}
