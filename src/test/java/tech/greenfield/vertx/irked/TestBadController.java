package tech.greenfield.vertx.irked;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;
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
	public void testInvalidFieldHandlerError(TestContext context) {
		deployController(new TestControllerBadField(), context.asyncAssertFailure());
	}

	@Test
	public void testInvalidMethodHandlerError(TestContext context) {
		deployController(new TestControllerBadMethod(), context.asyncAssertFailure());
	}

}
