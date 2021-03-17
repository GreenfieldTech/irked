package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Unauthorized;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public class TestWebSocket extends TestBase {
	private static final String PING = "ping";
	private static final String PONG = "pong";

	public class TestControllerTextPinger extends Controller {
		@WebSocket("/ping")
		MessageHandler pinger = m -> { if (m.toString().equals(PING)) m.reply(PONG); };
	}

	@Test
	public void testSimplePing(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerTextPinger(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/ping")
			.thenAccept(ws -> {
				ws.writeTextMessage(PING);
				ws.handler(buf -> {
					assertThat(buf.toString(), equalTo(PONG));
					ws.close();
					vertx.undeploy(s);
					async.flag();
				});
			}).exceptionally(failureHandler(context));
		}));
	}

	private static final byte[] BPING = new byte[] { 0x00, 0x01, 0x02, 0x03 };
	private static final byte[] BPONG = new byte[] { 0x70, 0x60, 0x50, 0x40 };
	public class TestControllerBinaryPinger extends Controller {
		@WebSocket("/binary-ping")
		MessageHandler pinger = m -> { 
			if (m.isBinary() && Arrays.equals(m.getBytes(), BPING)) m.reply(Buffer.buffer(BPONG)); 
		};
	}

	@Test
	public void testBinaryPing(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerBinaryPinger(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/binary-ping")
			.thenAccept(ws -> {
				ws.write(Buffer.buffer(BPING));
				ws.handler(buf -> {
					assertThat(buf.getBytes(), equalTo(BPONG));
					ws.close();
					vertx.undeploy(s);
					async.flag();
				});
			}).exceptionally(failureHandler(context));
		}));
	}
	
	public class TestControllerMethodPinger extends Controller {
		@WebSocket("/ping-method")
		void pinger(WebSocketMessage m) {
			m.reply(PONG);
		}
	}
	
	@Test
	public void testMethodPinger(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerMethodPinger(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/ping-method", HeadersMultiMap.httpHeaders().add("Authorization","ok"))
			.thenAccept(ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					assertThat(text, equalTo(PONG));
					ws.close();
					vertx.undeploy(s);
					async.flag();
				});
			}).exceptionally(failureHandler(context));
		}));
	}
	
	public class TestControllerAdvancedMethodPinger extends Controller {
		private class AutoReply extends Request {
			public AutoReply(RoutingContext outerContext) {
				super(outerContext);
			}
			public String getReply() {
				return PONG;
			}
		}
		@WebSocket("/ping-adv-method")
		void pinger(AutoReply req, WebSocketMessage m) {
			m.reply(req.getReply());
		}
		
		@Override
		protected Request getRequestContext(Request request) {
			return new AutoReply(request);
		}
	}
	
	@Test
	public void testAdvancedMethodPinger(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerAdvancedMethodPinger(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/ping-adv-method", HeadersMultiMap.httpHeaders().add("Authorization","ok"))
			.thenAccept(ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					assertThat(text, equalTo(PONG));
					ws.close();
					vertx.undeploy(s);
					async.flag();
				});
			}).exceptionally(failureHandler(context));
		}));
	}
	
	public class TestControllerAuthorizePing extends Controller {
		@OnFail
		@Endpoint("/with-auth")
		WebHandler failHandler = Request.failureHandler();
		
		@Get("/with-auth")
		WebHandler auth = req -> {
			if (req.request().getHeader("Authorization").equals("ok"))
				req.next();
			else
				req.fail(new Unauthorized());
		};
		
		@WebSocket("/with-auth")
		MessageHandler pinger = m -> {
			m.reply(PONG);
		};
	}
	
	@Test
	public void testAuthorizedPing(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerAuthorizePing(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/with-auth", HeadersMultiMap.httpHeaders().add("Authorization","ok"))
			.thenAccept(ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					assertThat(text, equalTo(PONG));
					ws.close();
					vertx.undeploy(s);
					async.flag();
				});
			}).exceptionally(failureHandler(context));
		}));
	}

	@Test
	public void testFailedToAuthorizedPing(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		deployController(new TestControllerAuthorizePing(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/with-auth", HeadersMultiMap.httpHeaders().add("Authorization","invalid"))
			.thenAccept(ws -> {
				context.failNow(new Exception("Invalid authorization should not succeed"));
			}).exceptionally(t -> {
				while (t instanceof RuntimeException && t.getCause() != null) t = t.getCause();
				if (t instanceof UpgradeRejectedException) {
					var e = (UpgradeRejectedException)t;
					assertThat(e.getStatus(), equalTo(401));
					vertx.undeploy(s);
					async.flag();
				} else
					context.failNow(t);
				return null;
			});
		}));
	}

	public class TestMessageHandlingFailure extends Controller {
		@WebSocket("/failures")
		void pinger(WebSocketMessage m) {
			throw new RuntimeException("Unexpected exception");
		}
	}
	
	@Test
	public void testMessageHandlingFailure(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		vertx.exceptionHandler(context::failNow);
		deployController(new TestMessageHandlingFailure(), vertx, context.succeeding(s -> {
			getClient(vertx).websocket(port, "localhost", "/failures", HeadersMultiMap.httpHeaders().add("Authorization","ok"))
			.thenAccept(ws -> {
				ws.closeHandler(v -> {
					assertThat(ws.closeStatusCode(), is(equalTo((short)1011)));
					assertThat(ws.closeReason(), is(equalTo("Unexpected exception")));
					async.flag();
				});
				ws.writeTextMessage(PING);
			}).exceptionally(failureHandler(context));
		}));
	}

}
