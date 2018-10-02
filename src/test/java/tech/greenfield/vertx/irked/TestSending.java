package tech.greenfield.vertx.irked;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;
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
		
	}

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
	}

	@Test
	public void testTextSending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendtext").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertEquals("hello world", body.toString());
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end();
	}

	@Test
	public void testBinarySending(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/sendbinary").exceptionHandler(t -> context.fail(t)).handler(res -> {
			context.assertEquals(200, res.statusCode(), "Request failed");
			res.exceptionHandler(t -> context.fail(t)).bodyHandler(body -> {
				try {
					context.assertTrue(Arrays.equals(data, body.getBytes()));
				} catch (Exception e) {
					context.fail(e);
				}
			});
			async.complete();
		}).end("{}");
	}

}
