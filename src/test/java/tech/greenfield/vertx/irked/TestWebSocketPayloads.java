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
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.WebSocket;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.server.Server;

public class TestWebSocketPayloads extends TestBase {

	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024;

	private static Logger log = LoggerFactory.getLogger(TestWebSocketPayloads.class);

	public class TestControllerForLargePayloads extends Controller {
		public TestControllerForLargePayloads(int scale) {
			log.info("Started generating payloads");
			request = generatePayload(scale);
			response = generatePayload(scale);
			checksumIn = checksumPayload(request);
			checksumOut = checksumPayload(response);
			log.info("done generating payloads");
		}
		
		Buffer checksumIn, checksumOut, request, response;
		@WebSocket("/payload")
		MessageHandler handler = m -> {
			log.info("Started handling payload request of " + m.length() + " bytes");
			m.request().vertx().executeBlocking(f -> f.complete(checksumPayload(m)), res -> {
				log.info("Completed log checksum");
				if (res.succeeded() && checksumIn.equals(res.result()))
					m.reply(response);
				else
					m.reply(response = Buffer.buffer());
				log.info("sent reply of " + response.length() + " bytes");
			});
		};
	}

	public Buffer generatePayload(int scale) {
		Random r = new Random();
		int size = 1024 * (4 + Math.abs(r.nextInt() % (2 * scale)));
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

	@Test
	public void testLargePayload(VertxTestContext context, Vertx vertx) {
		vertx.<TestControllerForLargePayloads>executeBlocking(p -> p.complete(new TestControllerForLargePayloads(1000)))
		.onSuccess(payloadTester(context, vertx))
		.onFailure(context::failNow);
	}

	@Test
	public void testLargerPayload(VertxTestContext context, Vertx vertx) {
		vertx.<TestControllerForLargePayloads>executeBlocking(p -> p.complete(new TestControllerForLargePayloads(10000)))
		.onSuccess(payloadTester(context, vertx))
		.onFailure(context::failNow);
	}

	@Test
	public void testLargestPayload(VertxTestContext context, Vertx vertx) {
		vertx.<TestControllerForLargePayloads>executeBlocking(p -> p.complete(new TestControllerForLargePayloads(50000)))
		.onSuccess(payloadTester(context, vertx))
		.onFailure(context::failNow);
	}

	private Handler<TestControllerForLargePayloads> payloadTester(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		return ctr -> {
			log.info("deploying verticle");
			Server.getHttpServerOptions().setMaxWebSocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE);
			deployController(ctr, vertx, context.succeeding(s -> {
				log.info("Starting websocket");
				getClient(vertx).websocket(port, "localhost", "/payload")
						.thenAccept(ws -> {
							log.info("Socket open, sending request");
							ws.writeBinaryMessage(ctr.request);
							ws.binaryMessageHandler(resp -> {
								log.info("Got a reply");
								assertThat(checksumPayload(resp), equalTo(ctr.checksumOut));
								log.info("Shutting down");
								ws.close();
								vertx.undeploy(s);
								async.flag();
							});
							log.info("Waiting for reply");
						}).exceptionally(failureHandler(context));
			}));
		};
	}

}
