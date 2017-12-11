package tech.greenfield.vertx.irked;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.NoContent;

public class TestMountController extends TestBase {

	public class ParentController extends Controller {
		@Get("/")
		WebHandler index = r -> {
			r.sendContent("index");
		};
		
		@Endpoint("/child")
		ChildController myChild = new ChildController();
		
		@Endpoint("/:param/paramChild")
		ParamChildController myParamChild = new ParamChildController();
	}
	
	public class ParamChildController extends Controller {
		@Get("/")
		WebHandler index = r -> {
			r.sendContent("param child index");
		};
		
		@Get("/test")
		WebHandler test = r -> {
			r.sendContent("param child test");
		};
	}
	
	public class ChildController extends Controller {
		@Get("/")
		WebHandler index = r -> {
			r.sendContent("child index");
		};
		
		@Get("/test")
		WebHandler update = r -> {
			r.sendContent("child test");
		};
	}

	@Before
	public void deployServer(TestContext context) {
		deployController(new ParentController(), context.asyncAssertSuccess());
	}

	@Test
	public void testParentIndex(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("index", context, async)).end();
	}
	
	@Test
	public void testChild(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/child")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("child index", context, async)).end();
	}

	@Test
	public void testChildIndex(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/child/")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("child index", context, async)).end();
	}

	@Test
	public void testChildTest(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/child/test")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("child test", context, async)).end();
	}

	@Test
	public void testParamChild(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/value/paramChild")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("param child index", context, async)).end();
	}

	@Test
	@Ignore("Wait for bug https://github.com/vert-x3/vertx-web/issues/786 to be fixed")
	public void testParamChildIndex(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/value/paramChild/")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("param child index", context, async)).end();
	}

	@Test
	public void testParamChildTest(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/value/paramChild/test")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler("param child test", context, async)).end();
	}

	private Handler<HttpClientResponse> compareBodyHandler(String message, TestContext context, Async f) {
		return r -> {
			context.assertEquals(200, r.statusCode(), "Failed to call consumes test '" + message + "'");
			r.exceptionHandler(context::fail).bodyHandler(body -> {
				context.assertEquals(message, body.toString());
				f.complete();
			});
		};
	}

}
