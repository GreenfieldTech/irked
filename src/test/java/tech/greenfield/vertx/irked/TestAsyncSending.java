package tech.greenfield.vertx.irked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.NoContent;
import tech.greenfield.vertx.irked.status.OK;

public class TestAsyncSending extends TestBase {

	@Rule
	public Timeout timeout = new Timeout(2000);
	
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

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testTextSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendtext").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("text/plain", res.getHeader("Content-Type"));
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertEquals("hello world", body.toString());
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testBinarySending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendbinary").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("application/octet-stream", res.getHeader("Content-Type"));
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertTrue(Arrays.equals(data, body.getBytes()));
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testJsonObjectSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendjsono").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("application/json", res.getHeader("Content-Type"));
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertNotNull(body.toJsonObject());
					context.assertEquals("bar",body.toJsonObject().getString("foo"));
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testJsonArraySending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendjsonl").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("application/json", res.getHeader("Content-Type"));
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertNotNull(body.toJsonArray());
					context.assertEquals(2,body.toJsonArray().size());
					context.assertEquals(2,body.toJsonArray().getInteger(1));
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testMappedObjectSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendmapped").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("application/json", res.getHeader("Content-Type"));
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertNotNull(body.toJsonObject());
					context.assertNotNull(body.toJsonObject().mapTo(TestValue.class));
					context.assertEquals("OK", body.toJsonObject().mapTo(TestValue.class).value);
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testStatusSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendempty").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(NoContent.code, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertEquals(0, body.length());
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCustomStatusSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendcustomok").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(OK.code, res.statusCode(), "Request failed");
			context.assertEquals("custom", res.getHeader("X-Custom"), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertNotNull(body.toJsonObject());
					context.assertEquals(true, body.toJsonObject().getBoolean("status"));
					context.assertEquals("Still OK", body.toJsonObject().getString("message"));
				} catch (Exception e) {
					context.fail(e);
				}
				async.complete();
			});
		}).end();
	}

}
