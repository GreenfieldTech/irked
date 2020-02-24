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
		Promise<TestControllerForLargePayloads> f1 = Promise.promise();
		vertx.executeBlocking(makeController(1000), f1);
		f1.future().setHandler(payloadTester(context, vertx));
	}

	@Test
	public void testLargerPayload(VertxTestContext context, Vertx vertx) {
		Promise<TestControllerForLargePayloads> f1 = Promise.promise();
		vertx.executeBlocking(makeController(10000), f1);
		f1.future().setHandler(payloadTester(context, vertx));
	}

	@Test
	public void testLargestPayload(VertxTestContext context, Vertx vertx) {
		Promise<TestControllerForLargePayloads> f1 = Promise.promise();
		vertx.executeBlocking(makeController(50000), f1);
		f1.future().setHandler(payloadTester(context, vertx));
	}

	private Handler<AsyncResult<TestControllerForLargePayloads>> payloadTester(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		return res -> {
			if (res.failed()) {
				log.info("Failde to init");
				context.failNow(res.cause());
				return;
			}

			log.info("deploying verticle");
			Server.getHttpServerOptions().setMaxWebsocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE);
			TestControllerForLargePayloads ctr = res.result();
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

	private Handler<Promise<TestControllerForLargePayloads>> makeController(int scale) {
		return f -> {
			log.info("Started generating payloads");
			TestControllerForLargePayloads ctr = new TestControllerForLargePayloads();
			ctr.request = generatePayload(scale);
			ctr.response = generatePayload(scale);
			ctr.checksumIn = checksumPayload(ctr.request);
			ctr.checksumOut = checksumPayload(ctr.response);
			log.info("done generating payloads");
			f.complete(ctr);
		};
	}

}
