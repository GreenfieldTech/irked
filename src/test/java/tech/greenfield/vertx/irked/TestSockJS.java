package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestSockJS extends TestBase {

	public class TestController extends Controller {
		
		@Get("/sockjs/*")
		io.vertx.ext.web.Router sockjs = SockJSHandler.create(Vertx.vertx()).socketHandler(sock -> {
			log.info("Connected socket");
			sock.handler(buf -> {
				log.info("Received {}", buf);
				sock.write(buf);
			});
		});

	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testInitialEndpont(VertxTestContext context, Vertx vertx) {
		Promise<io.vertx.core.buffer.Buffer> waitForResponse = Promise.promise();
		getClient(vertx).get(port, "localhost", "/sockjs/info").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			log.info("SockJS info: {}", o);
			return null;
		})
		.compose(__ -> getClient(vertx).websocket(port, "localhost", "/sockjs/websocket")
				.onSuccess(ws -> {
					ws.writeTextMessage("FOO");
					ws.handler(buf -> {
						ws.close();
						waitForResponse.complete(buf);
					});
				}))
		.compose(__ -> waitForResponse.future())
		.andThen((buf,t) -> {
			assertThat(buf.toString(), equalTo("FOO"));
		})
		.onComplete(context.succeedingThenComplete());
	}
}
