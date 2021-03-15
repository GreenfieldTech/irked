package tech.greenfield.vertx.irked;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;

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
		deployController(new TestControllerBadField(), vertx, context.succeedingThenComplete());
	}
	
	@Test
	public void testInvalidMethodHandlerError(VertxTestContext context, Vertx vertx) {
		deployController(new TestControllerBadMethod(), vertx, context.succeedingThenComplete());
	}

}
