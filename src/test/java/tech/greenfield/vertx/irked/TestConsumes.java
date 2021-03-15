package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Consumes;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.NotFound;

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
	public void testNoConsumes(VertxTestContext context, Vertx vertx) {
		deployController(new TestNoConsumes(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			getClient(vertx).post(port, "localhost", "/none")
					.putHeader("Content-Type", "application/octet-stream")
					.sendP()
					.thenAccept(compareBodyHandler(TestNoConsumes.message, context))
					.exceptionally(failureHandler(context))
					.thenRun(f::flag);
		}));
	}
	
	@Test
	public void testConsumesOne(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesOne(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			getClient(vertx).post(port, "localhost", "/one")
			.putHeader("Content-Type", "text/plain")
			.sendP()
			.thenAccept(compareBodyHandler(TestConsumesOne.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/one")
				.putHeader("Content-Type", "text/xml")
				.sendP())
			.thenAccept(verifyMissHandler(context))
			.exceptionally(failureHandler(context))
			.thenRun(f::flag);
		}));
	}
	
	@Test
	public void testConsumesMultiple(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesMultiple(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/multiple")
			.putHeader("Content-Type", "application/json")
			.sendP()
			.thenAccept(compareBodyHandler(TestConsumesMultiple.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "application/rss+xml")
				.sendP())
			.thenAccept(compareBodyHandler(TestConsumesMultiple.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "text/xml")
				.sendP())
			.thenAccept(verifyMissHandler(context))
			.exceptionally(failureHandler(context))
			.thenRun(f::flag);
		}));
	}
	
	@Test
	public void testConsumesGlob(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesGlob(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/glob")
			.putHeader("Content-Type", "image/png")
			.sendP()
			.thenAccept(compareBodyHandler(TestConsumesGlob.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/jpeg")
				.sendP())
			.thenAccept(compareBodyHandler(TestConsumesGlob.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/tiff")
				.sendP())
			.thenAccept(compareBodyHandler(TestConsumesGlob.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "text/xpm")
				.sendP())
			.thenAccept(verifyMissHandler(context))
			.exceptionally(failureHandler(context))
			.thenRun(f::flag);
		}));
	}
	
	@Test
	public void testConsumesWithFallback(VertxTestContext context, Vertx vertx) {
		deployController (new TestConsumesFallback(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/fallback")
			.putHeader("Content-Type", "application/xml")
			.sendP()
			.thenAccept(compareBodyHandler(TestConsumesFallback.message, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "application/octet-stream")
				.sendP())
			.thenAccept(compareBodyHandler(TestConsumesFallback.messagePartial, context))
			.thenCompose(v -> getClient(vertx).post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "text/plain")
				.sendP())
			.thenAccept(compareBodyHandler(TestConsumesFallback.messageFallback, context))
			.exceptionally(failureHandler(context))
			.thenRun(f::flag);
		}));
	}
	
	private Consumer<HttpResponse<Buffer>> compareBodyHandler(String message, VertxTestContext context) {
		return r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody(message));
		};
	}
	
	private Consumer<HttpResponse<Buffer>> verifyMissHandler(VertxTestContext context) {
		return r -> {
			assertThat(r, is(status(new NotFound())));
		};
	}

}
