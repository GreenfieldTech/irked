package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Consumes;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.UnsupportedMediaType;

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
					.send()
					.map(compareBodyHandler(TestNoConsumes.message, context))
					.onFailure(context::failNow)
					.onSuccess(flag(f));
		}));
	}
	
	@Test
	public void testConsumesOne(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesOne(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			getClient(vertx).post(port, "localhost", "/one")
			.putHeader("Content-Type", "text/plain")
			.send()
			.map(compareBodyHandler(TestConsumesOne.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/one")
				.putHeader("Content-Type", "text/xml")
				.send())
			.map(r -> {
				assertThat(r, is(status(new UnsupportedMediaType())));
				return null;
			})
			.onFailure(context::failNow)
			.onSuccess(flag(f));
		}));
	}
	
	@Test
	public void testConsumesMultiple(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesMultiple(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/multiple")
			.putHeader("Content-Type", "application/json")
			.send()
			.map(compareBodyHandler(TestConsumesMultiple.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "application/rss+xml")
				.send())
			.map(compareBodyHandler(TestConsumesMultiple.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/multiple")
				.putHeader("Content-Type", "text/xml")
				.send())
			.map(r -> {
				assertThat(r, is(status(UnsupportedMediaType.class)));
				return null;
			})
			.onFailure(context::failNow)
			.onSuccess(flag(f));
		}));
	}
	
	@Test
	public void testConsumesGlob(VertxTestContext context, Vertx vertx) {
		deployController(new TestConsumesGlob(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/glob")
			.putHeader("Content-Type", "image/png")
			.send()
			.map(compareBodyHandler(TestConsumesGlob.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/jpeg")
				.send())
			.map(compareBodyHandler(TestConsumesGlob.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "image/tiff")
				.send())
			.map(compareBodyHandler(TestConsumesGlob.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/glob")
				.putHeader("Content-Type", "text/xpm")
				.send())
			.map(r -> {
				assertThat(r, is(status(new UnsupportedMediaType())));
				return null;
			})
			.onFailure(context::failNow)
			.onSuccess(flag(f));
		}));
	}
	
	@Test
	public void testConsumesWithFallback(VertxTestContext context, Vertx vertx) {
		deployController (new TestConsumesFallback(), vertx, context.succeeding(s -> {
			Checkpoint f = context.checkpoint();
			
			getClient(vertx).post(port, "localhost", "/fallback")
			.putHeader("Content-Type", "application/xml")
			.send()
			.map(compareBodyHandler(TestConsumesFallback.message, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "application/octet-stream")
				.send())
			.map(compareBodyHandler(TestConsumesFallback.messagePartial, context))
			.compose(v -> getClient(vertx).post(port, "localhost", "/fallback")
				.putHeader("Content-Type", "text/plain")
				.send())
			.map(compareBodyHandler(TestConsumesFallback.messageFallback, context))
			.onFailure(context::failNow)
			.onSuccess(flag(f));
		}));
	}
	
	private Function<HttpResponse<Buffer>, Void> compareBodyHandler(String message, VertxTestContext context) {
		return r -> {
			assertThat(r, isOK());
			assertThat(r, hasBody(message));
			return null;
		};
	}
	
}
