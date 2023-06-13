package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.inject.Named;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

public class TestExceptionFailureControllerMisconfig extends TestBase {
	
	public class TestController extends Controller {
		@Get
		WebHandler correctFail = r -> r.fail(new IllegalStateException());
		
		@OnFail(exception = IllegalStateException.class)
		@Get
		public void brokenHandler(Request r, @Named("error") DecodeException error) {
			r.sendContent("This is a misconfiguration that should never succeed to deploy");
		};
		
	}

	@Test
	public void testToFail1(VertxTestContext context, Vertx vertx) {
		Checkpoint cp = context.checkpoint();
		Promise<Void> p = Promise.promise();
		deployController(new TestController(), vertx, context.failing(p::fail));
		p.future()
		.map(__ -> {
			throw new RuntimeException("Configuration should have failed");
		})
		.otherwise(t -> {
			assertThat(t, is(instanceOf(InvalidRouteConfiguration.class)));
			// Should be:
			 // Method brokenHandler contains parameters that cannot be resolved: Parameter 'DecodeException error' on failure handler does not match any @OnFail(exception) registration!"
			assertThat(t.getMessage(), containsString("Method brokenHandler contains parameters that cannot be resolved"));
			assertThat(t.getMessage(), matchesPattern(".*Parameter 'DecodeException \\w+' on failure handler does not match.*"));
			return null;
		})
		.onSuccess(__ -> cp.flag())
		.onFailure(t -> context.failNow(t));
	}

}
