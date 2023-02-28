package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.NoContent;
import tech.greenfield.vertx.irked.status.OK;

public class TestAsyncSending extends TestBase {

	public final static byte[] data = new byte[]{ 0x01, 0x02, 0x03, 0x04, 0x00, 0x00, 0x10, 0x20, 0x30, 0x40 };

	public static class TestValue {
		private String value = "OK";
		public void setValue(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}

	public class TestController extends Controller {

		@Get("/sendtext")
		public void text(Request r) {
			CompletableFuture.completedFuture("hello world")
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		}

		@Get("/sendbinary")
		public void binary(Request r) {
			CompletableFuture.completedFuture(Buffer.buffer(data))
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

		@Get("/sendjsono")
		public void jsono(Request r) {
			CompletableFuture.completedFuture(new JsonObject().put("foo", "bar"))
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

		@SuppressWarnings("serial")
		@Get("/sendjsonl")
		public void jsonl(Request r) {
			CompletableFuture.completedFuture(new ArrayList<Integer>() {{ add(1); add(2); }})
			.thenApply(l -> l.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll))
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

		@Get("/sendmapped")
		public void mapped(Request r) {
			CompletableFuture.completedFuture(new TestValue())
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

		@Get("/sendempty")
		public void empty(Request r) {
			CompletableFuture.completedFuture(new NoContent())
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

		@Get("/sendcustomok")
		public void customOK(Request r) {
			CompletableFuture.completedFuture(new OK("Still OK").addHeader("X-Custom", "custom"))
			.thenAccept(r::send)
			.exceptionally(r::handleFailure);
		};

	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testTextSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendtext")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("Content-Type"), equalTo("text/plain"));
			assertThat(res, hasBody("hello world"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testBinarySending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendbinary")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("Content-Type"), equalTo("application/octet-stream"));
			assertThat(res, hasBody(data));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testJsonObjectSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendjsono")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("Content-Type"), equalTo("application/json"));
			assertThat(res.bodyAsJsonObject(), notNullValue());
			assertThat(res.bodyAsJsonObject().getString("foo"), equalTo("bar"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testJsonArraySending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendjsonl")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("Content-Type"), equalTo("application/json"));
			assertThat(res.bodyAsJsonArray(), notNullValue());
			assertThat(res.bodyAsJsonArray().size(), equalTo(2));
			assertThat(res.bodyAsJsonArray().getInteger(1), equalTo(2));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testMappedObjectSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendmapped")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("Content-Type"), equalTo("application/json"));
			assertThat(res.bodyAsJsonObject(), notNullValue());
			assertThat(res.bodyAsJsonObject().mapTo(TestValue.class), notNullValue());
			assertThat(res.bodyAsJsonObject().mapTo(TestValue.class).value, equalTo("OK"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testStatusSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendempty")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(NoContent.code));
			assertThat(res, is(bodyEmpty()));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testCustomStatusSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendcustomok")
		.send()
		.map(res -> {
			assertThat(res.statusCode(), equalTo(OK.code));
			assertThat(res.getHeader("X-Custom"), equalTo("custom"));
			assertThat(res.bodyAsJsonObject(), notNullValue());
			assertThat(res.bodyAsJsonObject().getBoolean("status"), equalTo(true));
			assertThat(res.bodyAsJsonObject().getString("message"), equalTo("Still OK"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
