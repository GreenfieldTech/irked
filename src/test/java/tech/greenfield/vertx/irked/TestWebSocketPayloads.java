package tech.greenfield.vertx.irked;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import tech.greenfield.vertx.irked.annotations.WebSocket;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.server.Server;

public class TestWebSocketPayloads extends TestBase {

	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024;

	private static Logger log = LoggerFactory.getLogger(TestWebSocketPayloads.class);

	@Rule
	public Timeout timeout = Timeout.seconds(30);

	public class TestControllerForLargePayloads extends Controller {
		Buffer checksumIn, checksumOut, request, response;
		@WebSocket("/payload")
		MessageHandler handler = m -> {
			log.info("Started handling payload request of " + m.length() + " bytes");
			rule.vertx().executeBlocking(f -> f.complete(checksumPayload(m)), res -> {
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
	public void testLargePayload(TestContext context) {
		Future<TestControllerForLargePayloads> f1 = Future.future();
		rule.vertx().executeBlocking(makeController(1000), f1.completer());
		f1.setHandler(payloadTester(context));
	}

	@Test
	public void testLargerPayload(TestContext context) {
		Future<TestControllerForLargePayloads> f1 = Future.future();
		rule.vertx().executeBlocking(makeController(10000), f1.completer());
		f1.setHandler(payloadTester(context));
	}

	@Test
	public void testLargestPayload(TestContext context) {
		Future<TestControllerForLargePayloads> f1 = Future.future();
		rule.vertx().executeBlocking(makeController(50000), f1.completer());
		f1.setHandler(payloadTester(context));
	}

	private Handler<AsyncResult<TestControllerForLargePayloads>> payloadTester(TestContext context) {
		Async async = context.async();
		return res -> {
			if (res.failed()) {
				log.info("Failde to init");
				context.fail(res.cause());
				return;
			}

			log.info("deploying verticle");
			Server.getHttpServerOptions().setMaxWebsocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE);
			TestControllerForLargePayloads ctr = res.result();
			deployController(ctr, context.asyncAssertSuccess(s -> {
				log.info("Starting websocket");
				rule.vertx().createHttpClient(new HttpClientOptions().setIdleTimeout(0).setMaxWebsocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE))
						.websocketStream(port, "localhost", "/payload")
						.exceptionHandler(context::fail)
						.handler(ws -> {
							log.info("Socket open, sending request");
							ws.writeBinaryMessage(ctr.request);
							ws.binaryMessageHandler(resp -> {
								log.info("Got a reply");
								context.assertEquals(ctr.checksumOut, checksumPayload(resp));
								log.info("Shutting down");
								ws.close();
								rule.vertx().undeploy(s);
								async.complete();
							});
							log.info("Waiting for reply");
						});
			}));
		};
	}

	private Handler<Future<TestControllerForLargePayloads>> makeController(int scale) {
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
