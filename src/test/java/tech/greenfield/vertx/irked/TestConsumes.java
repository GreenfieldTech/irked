package tech.greenfield.vertx.irked;

import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.Consumes;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestConsumes extends TestBase {
	
	public static class TestNoConsumes extends Controller {
		public static String message = "no limit";
		@Endpoint("/none")
		WebHandler handler = r -> {
			r.sendContent(message);
		};
	}

	public static class TestConsumesOne extends Controller {
		public static String message = "consumes one";
		@Endpoint("/one")
		@Consumes("text/plain")
		WebHandler handler = r -> {
			r.sendContent(message);
		};
	}

	public static class TestConsumesMultiple extends Controller {
		public static String message = "consumes multiple";
		@Endpoint("/multiple")
		@Consumes("application/json")
		@Consumes("application/rss+xml")
		WebHandler handler = r -> {
			r.sendContent(message);
		};
	}
	
	public static class TestConsumesGlob extends Controller {
		public static String message = "consumes glob";
		@Endpoint("/glob")
		@Consumes("image/*")
		WebHandler handler = r -> {
			r.sendContent(message);
		};
	}
	
	public static class TestConsumesFallback extends Controller {
		public static String message = "consumes fallback - strict";
		public static String messagePartial = "consumes fallback - partial";
		public static String messageFallback = "consumes fallback - fallback";
		@Endpoint("/fallback")
		@Consumes("application/xml")
		WebHandler strict = r -> {
			r.sendContent(message);
		};
		
		@Endpoint("/fallback")
		@Consumes("application/*")
		WebHandler partial = r -> {
			r.sendContent(messagePartial);
		};
		
		@Endpoint("/fallback")
		WebHandler fallback = r -> {
			r.sendContent(messageFallback);
		};
	}

	@Test
	public void testNoConsumes(TestContext context) {
		deployController(new TestNoConsumes(), context.asyncAssertSuccess(s -> {
			Async f = context.async();
			getClient().post(port, "localhost", "/none")
					.putHeader("Content-Type", "application/octet-stream")
					.exceptionHandler(context::fail)
					.handler(compareBodyHandler(TestNoConsumes.message, context, f)).end();
		}));
	}
	
	@Test
	public void testConsumesOne(TestContext context) {
		deployController(new TestConsumesOne(), context.asyncAssertSuccess(s -> {
			Async f1 = context.async();
			getClient().post(port, "localhost", "/one")
			.putHeader("Content-Type", "text/plain")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler(TestConsumesOne.message, context, f1)).end();
			
			Async f2 = context.async();
			f1.handler(r -> {
				getClient().post(port, "localhost", "/one")
				.putHeader("Content-Type", "text/xml")
				.exceptionHandler(context::fail)
				.handler(verifyMissHandler(context, f2)).end();
			});
		}));
	}
	
	@Test
	public void testConsumesMultiple(TestContext context) {
		deployController(new TestConsumesMultiple(), context.asyncAssertSuccess(s -> {
			Async f1 = context.async();
			
			getClient().post(port, "localhost", "/multiple")
			.putHeader("Content-Type", "application/json")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler(TestConsumesMultiple.message, context,f1)).end();
			
			Async f2 = context.async();
			f1.handler(r -> {
				getClient().post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "application/rss+xml")
				.exceptionHandler(context::fail)
				.handler(compareBodyHandler(TestConsumesMultiple.message, context, f2)).end();
			});
			
			Async f3 = context.async();
			f2.handler(r -> {
				getClient().post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "text/xml")
				.exceptionHandler(context::fail)
				.handler(verifyMissHandler(context, f3)).end();
			});
		}));
	}
	
	@Test
	public void testConsumesGlob(TestContext context) {
		deployController(new TestConsumesGlob(), context.asyncAssertSuccess(s -> {
			Async f1 = context.async();
			
			getClient().post(port, "localhost", "/glob")
			.putHeader("Content-Type", "image/png")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler(TestConsumesGlob.message, context, f1)).end();

			Async f2 = context.async();
			f1.handler(r -> {
				getClient().post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/jpeg")
				.exceptionHandler(context::fail)
				.handler(compareBodyHandler(TestConsumesGlob.message, context,f2)).end();
			});
			
			Async f3 = context.async();
			f2.handler(r -> {
				getClient().post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/tiff")
				.exceptionHandler(context::fail)
				.handler(compareBodyHandler(TestConsumesGlob.message, context, f3)).end();
			});

			Async f4 = context.async();
			f3.handler(r -> {
				getClient().post(port, "localhost", "/glob")
				.putHeader("Content-Type", "text/xpm")
				.exceptionHandler(context::fail)
				.handler(verifyMissHandler(context, f4)).end();
			});
		}));
	}
	
	@Test
	public void testConsumesWithFallback(TestContext context) {
		deployController (new TestConsumesFallback(), context.asyncAssertSuccess(s -> {
			Async f1 = context.async();
			
			getClient().post(port, "localhost", "/fallback")
			.putHeader("Content-Type", "application/xml")
			.exceptionHandler(context::fail)
			.handler(compareBodyHandler(TestConsumesFallback.message, context, f1)).end();
	
			Async f2 = context.async();
			f1.handler(r -> {
				getClient().post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "application/octet-stream")
				.exceptionHandler(context::fail)
				.handler(compareBodyHandler(TestConsumesFallback.messagePartial, context, f2)).end();
			});
			
			Async f3 = context.async();
			f2.handler(r -> {
				getClient().post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "text/plain")
				.exceptionHandler(context::fail)
				.handler(compareBodyHandler(TestConsumesFallback.messageFallback, context, f3)).end();
			});
		}));
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
	
	private Handler<HttpClientResponse> verifyMissHandler(TestContext context, Async f) {
		return r -> {
			context.assertTrue(r.statusCode() == 404 || r.statusCode() == 415, "Should have received an error for incorrect type");
			f.complete();
		};
	}

}
