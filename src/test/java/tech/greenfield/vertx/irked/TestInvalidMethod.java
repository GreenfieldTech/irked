package tech.greenfield.vertx.irked;

import org.junit.Test;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

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
	 */
	@Test
	public void privateMethod(TestContext context) {
		Async async = context.async();
		deployController(new PrivateController(), context.asyncAssertSuccess(deploymentID -> {
			getClient().get(port, "localhost", "/private").exceptionHandler(t -> context.fail(t)).handler(res -> {
				res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
					context.assertEquals(200, res.statusCode(), "Request failed: " + body);
					context.assertEquals("OK", body.toString());
					rule.vertx().undeploy(deploymentID, context.asyncAssertSuccess());
					async.complete();
				});
			}).end();
		}));
	}
	
	@Test
	public void noArgs(TestContext context) {
		deployController(new NoArgController(), context.asyncAssertFailure(t -> 
			context.assertEquals(t.getClass(), InvalidRouteConfiguration.class)
		));
	}

	@Test
	public void tooManyArgs(TestContext context) {
		deployController(new ManyArgsController(), context.asyncAssertFailure(t -> 
			context.assertEquals(t.getClass(), InvalidRouteConfiguration.class)
		));
	}
	
	@Test
	public void routingMethodWantsWrongType(TestContext context) {
		Async async = context.async();
		deployController(new InvalidTypeController(), context.asyncAssertSuccess(deploymentID -> {
			
			getClient().get(port, "localhost", "/invalid").exceptionHandler(t -> context.fail(t)).handler(res -> {
				res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
					context.assertEquals(500, res.statusCode(), "Request failed: " + body);
					context.assertTrue(body.toJsonObject().getString("message").startsWith("Invalid request handler"));
					rule.vertx().undeploy(deploymentID, context.asyncAssertSuccess());
					async.complete();
				});
			}).end();
		}));
	}
}
