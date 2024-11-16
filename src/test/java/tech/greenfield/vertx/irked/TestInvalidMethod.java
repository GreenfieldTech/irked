package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.status.InternalServerError;

/**
 * A set of negative tests to see how Irked handle controller with bad routing methods
 * @author odeda
 */
public class TestInvalidMethod extends TestBase {

	public class PrivateController extends Controller {
		@Get("/private")
		private void test(Request r) {
			r.sendContent("OK");
		}
	}

	public class NoArgController extends Controller {
		@Get("/noargs")
		private void test() {
		}
	}

	public class ManyArgsController extends Controller {
		@Get("/toomany")
		private void test(Request r, int id) {
			r.sendContent("OK");
		}
	}

	public class InvalidTypeController extends Controller {
		public class ValidCustomRequest extends Request {
			public ValidCustomRequest(RoutingContext outerContext) {
				super(outerContext);
			}}
		public class InvalidCustomRequest extends Request {
			public InvalidCustomRequest(RoutingContext outerContext) {
				super(outerContext);
			}}
		
		@Get("/invalid")
		private void test(InvalidCustomRequest r) {
			r.sendContent("OK");
		}
		
		@OnFail
		@Endpoint("/*")
		public WebHandler fail = r -> {
			r.sendError(HttpError.toHttpError(r));
		};

		@Override
		protected Request getRequestContext(Request request) {
			return new ValidCustomRequest(request);
		}
	}

	/**
	 * This test should actually succeed, because we override the permissions. We're bad like that
	 * @param context
	 * @param vertx 
	 */
	@Test
	public void privateMethod(VertxTestContext context, Vertx vertx) {
		deployController(new PrivateController(), vertx, context.succeeding(deploymentID -> {
			getClient(vertx).get(port, "localhost", "/private").send().compose(res -> {
				assertThat(res, isSuccess());
				assertThat(res.bodyAsString(), equalTo("OK"));
				return vertx.undeploy(deploymentID);
			})
			.andThen(context.succeedingThenComplete());
		}));
	}
	
	@Test
	public void noArgs(VertxTestContext context, Vertx vertx) {
		deployController(new NoArgController(), vertx, context.failing(t -> {
			assertThat(t, is(instanceOf(InvalidRouteConfiguration.class)));
			context.completeNow();
		}));
	}

	@Test
	public void tooManyArgs(VertxTestContext context, Vertx vertx) {
		deployController(new ManyArgsController(), vertx, context.failing(t -> {
			assertThat(t, is(instanceOf(InvalidRouteConfiguration.class)));
			context.completeNow();
		}));
	}
	
	@Test
	public void routingMethodWantsWrongType(VertxTestContext context, Vertx vertx) {
		deployController(new InvalidTypeController(), vertx, context.succeeding(deploymentID -> {
			
			getClient(vertx).get(port, "localhost", "/invalid").send().compose(res -> {
				assertThat(res, is(status(new InternalServerError())));
				assertThat(res.bodyAsJsonObject().getString("message"), startsWith("Invalid request handler"));
				return vertx.undeploy(deploymentID);
			})
			.andThen(context.succeedingThenComplete());
		}));
	}
}
