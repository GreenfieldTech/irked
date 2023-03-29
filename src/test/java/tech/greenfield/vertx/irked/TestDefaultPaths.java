package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Connect;
import tech.greenfield.vertx.irked.annotations.Delete;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Head;
import tech.greenfield.vertx.irked.annotations.Patch;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.MethodNotAllowed;
import tech.greenfield.vertx.irked.status.OK;

public class TestDefaultPaths extends TestBase {

	public class SubController1 extends Controller {
		@Put
		public void put(Request r) {
			r.sendContent("PUT");
		}

		@Patch
		public void patch(Request r) {
			r.sendContent("PATCH");
		}
		
	}
	
	public class SubController2 extends Controller {
		@Endpoint
		public void allRequests(Request r) {
			r.sendContent("All");
		}
	}
	
	public class TestController extends Controller {
		
		@Head
		public void head(Request r) {
			r.response().putHeader("Was-Handled", "HEAD");
			r.send(new OK()); // client will not read the body of a HEAD response, so don't bother
		}

		@Get
		public void get(Request r) {
			r.sendContent("GET");
		}

		@Post
		public void post(Request r) {
			r.sendContent("POST");
		}

		@Endpoint("/sub1")
		SubController1 sub1 = new SubController1();

		@Connect
		public void connect(Request r) {
			r.sendContent("CONNECT");
		}

		@Delete
		public void delete(Request r) {
			r.sendContent("DELETE");
		}

	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
		System.out.println("Running on port " + port);
	}

	@Test
	public void testHead(VertxTestContext context, Vertx vertx) {
		getClient(vertx).head(port, "localhost", "/foo").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.getHeader("Was-Handled"), is(equalTo("HEAD")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testGet(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/bar").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toString(), is(equalTo("GET")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPost(VertxTestContext context, Vertx vertx) {
		getClient(vertx).post(port, "localhost", "/fooz").send("stuff").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toString(), is(equalTo("POST")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPut(VertxTestContext context, Vertx vertx) {
		getClient(vertx).put(port, "localhost", "/sub1").send("stuff").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toString(), is(equalTo("PUT")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPutToFail(VertxTestContext context, Vertx vertx) {
		getClient(vertx).put(port, "localhost", "/").send("stuff").map(res -> {
			assertThat(res, is(status(new MethodNotAllowed())));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPatch(VertxTestContext context, Vertx vertx) {
		getClient(vertx).patch(port, "localhost", "/sub1").send("stuff").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toString(), is(equalTo("PATCH")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPatchToFail(VertxTestContext context, Vertx vertx) {
		getClient(vertx).patch(port, "localhost", "/").send("stuff").map(res -> {
			assertThat(res, is(status(new MethodNotAllowed())));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testDelete(VertxTestContext context, Vertx vertx) {
		getClient(vertx).delete(port, "localhost", "/normal").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toString(), is(equalTo("DELETE")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

}
