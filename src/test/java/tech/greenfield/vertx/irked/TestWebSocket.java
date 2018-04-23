package tech.greenfield.vertx.irked;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebsocketRejectedException;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Unauthorized;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public class TestWebSocket extends TestBase {
	private static final String PING = "ping";
	private static final String PONG = "pong";

	@Rule
	public Timeout timeout = Timeout.seconds(2);
	
	public class TestControllerTextPinger extends Controller {
		@WebSocket("/ping")
		MessageHandler pinger = m -> { if (m.toString().equals(PING)) m.reply(PONG); };
	}

	@Test
	public void testSimplePing(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerTextPinger(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/ping", ws -> {
				ws.writeTextMessage(PING);
				ws.handler(buf -> {
					context.assertEquals(PONG, buf.toString());
					ws.close();
					rule.vertx().undeploy(s);
					async.complete();
				});
			}, context::fail);
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
	public void testBinaryPing(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerBinaryPinger(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/binary-ping", ws -> {
				ws.write(Buffer.buffer(BPING));
				ws.handler(buf -> {
					context.assertTrue(Arrays.equals(BPONG, buf.getBytes()));
					ws.close();
					rule.vertx().undeploy(s);
					async.complete();
				});
			}, context::fail);
		}));
	}
	
	public class TestControllerMethodPinger extends Controller {
		@WebSocket("/ping-method")
		void pinger(WebSocketMessage m) {
			m.reply(PONG);
		}
	}
	
	@Test
	public void testMethodPinger(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerMethodPinger(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/ping-method", new VertxHttpHeaders().add("Authorization","ok"), ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					context.assertEquals(PONG, text);
					ws.close();
					rule.vertx().undeploy(s);
					async.complete();
				});
			}, context::fail);
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
	public void testAdvancedMethodPinger(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerAdvancedMethodPinger(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/ping-adv-method", new VertxHttpHeaders().add("Authorization","ok"), ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					context.assertEquals(PONG, text);
					ws.close();
					rule.vertx().undeploy(s);
					async.complete();
				});
			}, context::fail);
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
	public void testAuthorizedPing(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerAuthorizePing(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/with-auth", new VertxHttpHeaders().add("Authorization","ok"), ws -> {
				ws.writeTextMessage(PING);
				ws.textMessageHandler(text -> {
					context.assertEquals(PONG, text);
					ws.close();
					rule.vertx().undeploy(s);
					async.complete();
				});
			}, context::fail);
		}));
	}

	@Test
	public void testFailedToAuthorizedPing(TestContext context) {
		Async async = context.async();
		deployController(new TestControllerAuthorizePing(), context.asyncAssertSuccess(s -> {
			getClient().websocket(port, "localhost", "/with-auth", new VertxHttpHeaders().add("Authorization","invalid"), ws -> {
				context.fail("Invalid authorization should not succeed");
			}, t -> {
				if (t instanceof WebsocketRejectedException) {
					WebsocketRejectedException e = (WebsocketRejectedException)t;
					context.assertEquals(401, e.getStatus());
					rule.vertx().undeploy(s);
					async.complete();
				} else
					context.fail(t);
			});
		}));
	}

}
