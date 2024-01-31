package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestControllerSubclassing extends TestBase {

	public class ParentController extends Controller {
		@Get("/")
		WebHandler index = r -> {
			r.sendContent("parent");
		};
	}
	
	public class ChildController extends ParentController {
		@Get("/child")
		WebHandler childIndex = r -> {
			r.sendContent("child");
		};
	}
	
	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new ChildController(), vertx, context.succeedingThenComplete());
	}

	@Test
	@Timeout(value = 5, timeUnit = TimeUnit.MINUTES)
	public void testParentIndex(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/")
			.send().map(r -> {
				assertThat(r, isSuccess());
				assertThat(r, hasBody("parent"));
				return null;
			})
			.onFailure(context::failNow)
			.onSuccess(flag(async));
	}
	
	@Test
	@Timeout(value = 5, timeUnit = TimeUnit.MINUTES)
	public void testChild(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/child")
			.send()
			.map(r -> {
				assertThat(r, isSuccess());
				assertThat(r, hasBody("child"));
				return null;
			})
			.onFailure(context::failNow)
			.onSuccess(flag(async));
	}

}
