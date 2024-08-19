package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Post;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestEncoding extends TestBase {

	public final static byte[] data = new byte[]{ 0x01, 0x02, 0x03, 0x04, 0x00, 0x00, 0x10, 0x20, 0x30, 0x40 };
	
	public class PlainTestController extends Controller {
		@Get("/")
		public void test(Request r) {
			r.sendJSON(new JsonObject().put("hello", "world"));
		}
	}

	public class PrettyTestController extends Controller {
		@Override
		protected Request getRequestContext(Request request) {
			return request.setJsonEncoding(true);
		}
		
		@Get("/")
		public void test(Request r) {
			r.sendJSON(new JsonObject().put("hello", "world"));
		}
		
		@Endpoint("/subcontroller")
		SubPrettyTestController sub = new SubPrettyTestController();
	}
	
	public class SubPrettyTestController extends Controller {
		@Post("/")
		WebHandler test = r -> r.sendJSON(new JsonArray().add(new JsonObject().put("name", "foo")).add("bar"));
	}
	
	public class TestController extends Controller {
		@Endpoint("/plain")
		PlainTestController plain = new PlainTestController();
		
		@Endpoint("/pretty")
		PrettyTestController pretty = new PrettyTestController();
	}
	
	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testPlainResult(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/plain").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), equalTo("{\"hello\":\"world\"}"));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}

	@Test
	public void testPrettyResult(VertxTestContext context, Vertx vertx) {
		getClient(vertx).get(port, "localhost", "/pretty").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), equalTo("{\n  \"hello\" : \"world\"\n}"));
			return null;
		})
		.compose(__ -> getClient(vertx).post(port, "localhost", "/pretty/subcontroller").send("hello"))
		.map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), equalTo("[ {\n  \"name\" : \"foo\"\n}, \"bar\" ]"));
			return null;
		})
		.onComplete(context.succeedingThenComplete());
	}


}
