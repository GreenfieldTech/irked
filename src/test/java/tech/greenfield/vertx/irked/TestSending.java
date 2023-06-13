package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Created;
import tech.greenfield.vertx.irked.status.OK;

public class TestSending extends TestBase {

	public final static byte[] data = new byte[]{ 0x01, 0x02, 0x03, 0x04, 0x00, 0x00, 0x10, 0x20, 0x30, 0x40 };

	public class TestController extends Controller {
		
		@Get("/sendtext")
		public void text(Request r) {
			r.sendContent("hello world", new OK(), "text/plain");
		}

		@Get("/sendbinary")
		public void binary(Request r) {
			r.sendContent(Buffer.buffer(data), new OK(), "application/octet-stream");
		};
		
		@Get("/sendlist")
		public void list(Request r) {
			List<String> l = new ArrayList<>();
			l.add("hello");
			l.add("world");
			r.send(l);
		}
		
		@Get("/sendstream")
		public void stream(Request r) {
			r.send(Stream.of("hello", "world"));
		}
		
		@Get("/send-created")
		public void created(Request r) {
			r.response().setStatusCode(Created.code);
			r.send("Created");
		}
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testTextSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendtext").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), equalTo("hello world"));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testBinarySending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendbinary").send("{}").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().getBytes(), equalTo(data));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testListSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendlist").send("{}").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toJsonArray(), is(equalTo(new JsonArray().add("hello").add("world"))));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}


	@Test
	public void testStreamSending(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sendstream").send("{}").map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.body().toJsonArray(), is(equalTo(new JsonArray().add("hello").add("world"))));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}
	
	@Test
	public void testSendHonorsResponseStatusCode(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/send-created").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.statusCode(), is(equalTo(Created.code)));
			assertThat(res.bodyAsString(), is(equalTo("Created")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
