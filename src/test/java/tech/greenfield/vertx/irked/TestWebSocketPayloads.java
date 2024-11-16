package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.WebSocket;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.server.Server;

public class TestWebSocketPayloads extends TestBase {

	// server max message
	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024; // 1G. Frame size is 64K

	private static Logger log = LoggerFactory.getLogger(TestWebSocketPayloads.class);

	public class TestControllerForLargePayloads extends Controller {
		
		Buffer checksumIn, checksumOut, request, response;
		
		public TestControllerForLargePayloads(int size) {
			log.info("Started generating payloads");
			request = generatePayload(size);
			response = generatePayload(size);
			checksumIn = checksumPayload(request);
			checksumOut = checksumPayload(response);
			log.info("done generating payloads");
		}
		
		public Buffer generatePayload(int size) {
			Random r = new Random();
			byte[] payload = new byte[size];
			r.nextBytes(payload);
			return Buffer.buffer(payload);
		}

		public Buffer checksumPayload(Buffer payload) {
			try {
				return Buffer.buffer(MessageDigest.getInstance("MD5").digest(payload.getBytes()));
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
		}

		@WebSocket("/msg-handler")
		MessageHandler handler = m -> {
			log.info("Started handling payload request of " + m.length() + " bytes");
			m.request().vertx().executeBlocking(() -> checksumPayload(m))
			.onComplete(res -> {
				log.info("Completed log checksum");
				if (res.succeeded() && checksumIn.equals(res.result()))
					m.reply(response);
				else
					m.reply(response = Buffer.buffer());
				log.info("sent reply of " + response.length() + " bytes");
			});
		};
		
		@Get("/web-handler")
		WebHandler testhandler = r -> {
			log.info("Starting web handler manual upgrade {}", r.needUpgrade("websocket"));
			r.request().toWebSocket().onFailure(t -> log.error("Failed to upgrade",t))
			.onSuccess(this::websocketHandler);
		};
		
		void websocketHandler(ServerWebSocket ws) {
			log.info("setting up ws handler");
			ws.binaryMessageHandler(data -> {
				log.info("Got a binary message", data);
				ws.writeBinaryMessage(response);
				ws.writeTextMessage("foo");
			});
			ws.textMessageHandler(text -> {
				log.info("Got a text message {}", text);
				ws.writeTextMessage("ok");
			});
			ws.exceptionHandler(t -> {
				log.error("Error handling ws messages",t);
				ws.close((short) 500, "Error handling incoming frame: " + t.getMessage());
			});
			log.info("Accepted socket");
		}
	}

	@Test
	public void testLargePayloadMsg(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(1000);
		payloadTester(ctr, context, vertx, true);
	}

	@Test
	public void testLargerPayloadMsg(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(4000);
		payloadTester(ctr, context, vertx, true);
	}

	@Test
	public void testLargestPayloadMsg(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(8000);
		payloadTester(ctr, context, vertx, true);
	}

	@Test
	public void testLargePayload(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(1000);
		payloadTester(ctr, context, vertx, false);
	}

	@Test
	public void testLargerPayload(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(40000);
		payloadTester(ctr, context, vertx, false);
	}

	@Test
	public void testLargestPayload(VertxTestContext context, Vertx vertx) {
		var ctr = new TestControllerForLargePayloads(70000);
		payloadTester(ctr, context, vertx, false);
	}

	private void payloadTester(TestControllerForLargePayloads ctr, VertxTestContext context, Vertx vertx, boolean wsmsg) {
		Checkpoint async = context.checkpoint();
		log.info("deploying verticle");
		Server.getHttpServerOptions().setMaxWebSocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE);
		deployController(ctr, vertx, context.succeeding(s -> {
			sendWebSocketMessage(vertx, ctr, wsmsg).onSuccess(v -> {
				log.info("Shutting down");
				vertx.undeploy(s);
				async.flag();
			})
			.onFailure(t -> {
				failureHandler(context).apply(t);
			});
		}));
	}

	private Future<Void> sendWebSocketMessage(Vertx vertx, TestControllerForLargePayloads ctr, boolean wsmsg) {
		Promise<Void> res = Promise.promise();
		log.info("Starting websocket");
		getClient(vertx).websocket(port, "localhost", wsmsg ? "/msg-handler" : "/web-handler")
				.thenAccept(ws -> {
					log.info("Socket open, sending request");
					ws.binaryMessageHandler(resp -> {
						log.info("Got a reply");
						assertThat(ctr.checksumPayload(resp), equalTo(ctr.checksumOut));
						ws.close();
						res.complete();
					})
					.exceptionHandler(t -> {
						log.error("Error in client ws",t);
						res.fail(t);
					})
					.writeBinaryMessage(ctr.request)
					.onFailure(e -> log.error("Failed sending message",e));
					log.info("Waiting for reply");
				}).exceptionally(t -> {
					res.fail(t);
					return null;
				});
		return res.future();
	}

	@Test
	public void testClassicWebSocketAPI(VertxTestContext context, Vertx vertx) {
		var cp = context.checkpoint();
		var ctr = new TestControllerForLargePayloads(100);
		vertx.createHttpServer().requestHandler(req -> {
			req.pause();
			req.toWebSocket().onFailure(t -> log.error("Failed to upgrade",t))
			.onSuccess(ws -> {
//				ws.accept();
				ws.binaryMessageHandler(data -> {
					log.info("Got a binary message", data);
					ws.writeBinaryMessage(ctr.response);
				});
				ws.textMessageHandler(text -> {
					log.info("Got a text message {}", text);
				});
			});
		}).listen(port).toCompletionStage().thenCompose(server -> sendWebSocketMessage(vertx, ctr, false)
				.toCompletionStage().thenRun(cp::flag))
		.exceptionally(t -> {
			context.failNow(t);
			return null;
		});
	}
}
