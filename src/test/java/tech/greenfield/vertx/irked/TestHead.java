package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Head;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Imateapot;
import tech.greenfield.vertx.irked.status.MethodNotAllowed;
import tech.greenfield.vertx.irked.status.OK;

public class TestHead extends TestBase {

	public class TestController extends Controller {
		
		@Head("/sendtext")
		@Get("/sendtext")
		public void text(Request r) {
			r.sendContent("hello world", new OK(), "text/plain");
		}

		@Head("/headcheck")
		public void stream(Request r) {
			r.send(new Imateapot());
		}
		
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
		System.out.println("Running on port " + port);
	}

	@Test
	public void testHeadCheck(VertxTestContext context, Vertx vertx) {
		getClient(vertx).head(port, "localhost", "/headcheck").send().map(res -> {
			assertThat(res, is(status(new Imateapot())));
			assertThat(res.body(), is(nullValue()));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testNoHeadCheck(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/headcheck").send().map(res -> {
			assertThat(res, is(status(new MethodNotAllowed())));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testHandleGet(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/sendtext").send().map(res -> {
			assertThat(res, isOK());
			assertThat(res.headers().get("content-type"), is(equalTo("text/plain")));
			assertThat(res.bodyAsString(), is(equalTo("hello world")));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testHandleHead(VertxTestContext context, Vertx vertx) {
		getClient(vertx).head(port, "localhost", "/sendtext").send().map(res -> {
			assertThat(res, isOK());
			assertThat(res.headers().get("content-type"), is(equalTo("text/plain")));
			assertThat(res.body(), is(nullValue()));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

}
