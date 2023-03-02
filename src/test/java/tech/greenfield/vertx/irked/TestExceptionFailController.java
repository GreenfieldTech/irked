package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Imateapot;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class TestExceptionFailController extends TestBase {

	public class SubFailController extends Controller {
		@Get("/throw-illegal")
		WebHandler throwIllegal = r -> {
			throw new IllegalArgumentException();
		};
		
		@Get("/throw-decode")
		WebHandler throwDecode = r -> {
			throw new DecodeException();
		};
		
		@OnFail(exception = IllegalArgumentException.class)
		@OnFail(exception = DecodeException.class)
		@Endpoint
		WebHandler multiFailure = r -> {
			r.send(new Imateapot());
		};
	}
	public class TestController extends Controller {
		@Get("/correctfail")
		WebHandler correctFail = r -> {
			r.fail(new IllegalStateException());
		};
		
		@Get("/alsocorrectfail")
		WebHandler wrongFail1 = r -> {
			throw new IllegalStateException();
		};
		
		@Get("/yetanothercorrectfail")
		WebHandler wrongFail2 = r -> {
			r.fail(500, new IllegalStateException());
		};
		
		@Get("/wrongfail")
		WebHandler wrongFail = r -> {
			r.fail(new IllegalAccessError());
		};
		
		@Endpoint("/sub")
		SubFailController subfail = new SubFailController();
		
		@OnFail(exception = IllegalStateException.class)
		@Endpoint("/*")
		WebHandler failureHandler = r -> {
			r.sendJSON(new JsonObject().put("success", false));
		};

		@OnFail
		@Endpoint("/*")
		WebHandler defaultFailHandler = r -> {
			r.sendJSON(new JsonObject().put("did-default", true),
					new InternalServerError());
		};
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testToFail1(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/correctfail").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), is(equalTo(Boolean.FALSE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToFail2(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/alsocorrectfail").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), is(equalTo(Boolean.FALSE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToFail3(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/yetanothercorrectfail").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), is(equalTo(Boolean.FALSE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToWrongFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/wrongfail").send().map(res -> {
			assertThat(res, is(status(new InternalServerError())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("did-default"), is(equalTo(Boolean.TRUE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testSubMultiFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/sub/throw-illegal").send().map(res -> {
			assertThat(res, is(status(new Imateapot())));
			return null;
		})
		.compose(__ -> getClient(vertx).get(port, "localhost", "/sub/throw-decode").send())
		.map(res -> {
			assertThat(res, is(status(new Imateapot())));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
