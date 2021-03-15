package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;

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

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new ParentController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testParentIndex(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/")
			.sendP().thenAccept(r -> {
				assertThat(r, isOK());
				assertThat(r, hasBody("index"));
			})
			.exceptionally(failureHandler(context))
			.thenRun(async::flag);
	}
	
	@Test
	public void testChild(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/child")
			.sendP()
			.thenAccept(r -> {
				assertThat(r, isOK());
				assertThat(r, hasBody("child index"));
			})
			.exceptionally(failureHandler(context))
			.thenRun(async::flag);
	}

	@Test
	public void testChildIndex(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/child/")
		.sendP()
		.thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("child index"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testChildTest(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/child/test")
		.sendP()
		.thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("child test"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testParamChild(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/value/paramChild")
		.sendP()
		.thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("param child index"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	@Disabled("Wait for bug https://github.com/vert-x3/vertx-web/issues/786 to be fixed")
	public void testParamChildIndex(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/value/paramChild/")
		.sendP()
		.thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("param child index"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	@Test
	public void testParamChildTest(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/value/paramChild/test")
		.sendP()
		.thenAccept(r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody("param child test"));
		})
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

}
