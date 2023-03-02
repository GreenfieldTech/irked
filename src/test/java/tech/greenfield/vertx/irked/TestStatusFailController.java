package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Endpoint;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.status.Created;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class TestStatusFailController extends TestBase {

	public class TestController extends Controller {
		@OnFail(status = 501)
		@Endpoint("/*")
		WebHandler failureHandler = r -> {
			r.sendJSON(new JsonObject().put("success", false));
		};

		@OnFail()
		@Endpoint("/*")
		WebHandler defaultFailHandler = r -> {
			r.sendJSON(new JsonObject().put("did-default", true),
					new InternalServerError());
		};

		@Get("/correctfail")
		WebHandler correctFail = r -> {
			r.fail(501);
		};
		
		@Get("/wrongfail1")
		WebHandler wrongFail1 = r -> {
			throw new RuntimeException("wrong");
		};
		
		@Get("/wrongfail2")
		WebHandler wrongFail2 = r -> {
			r.fail(500);
		};
		
		@Get("/wrongfail3")
		WebHandler wrongFail3 = r -> {
			r.fail(new IllegalAccessError());
		};
		
		@Get("/httpexceptionfail")
		WebHandler httpExceptionFail = r -> {
			r.fail(new HttpException(501, "This should work?"));
		};
		
		@Get("/notfailing")
		WebHandler ok = r -> r.sendContent("OK", new Created());
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testToFail(VertxTestContext context, Vertx vertx) {
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
	public void testToWrongFail1(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/wrongfail1").send().map(res -> {
			assertThat(res, is(status(new InternalServerError())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("did-default"), is(equalTo(Boolean.TRUE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToWrongFail2(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/wrongfail2").send().map(res -> {
			assertThat(res, is(status(new InternalServerError())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("did-default"), is(equalTo(Boolean.TRUE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToWrongFail3(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/wrongfail3").send().map(res -> {
			assertThat(res, is(status(new InternalServerError())));
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("did-default"), is(equalTo(Boolean.TRUE)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testForExceptionFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/httpexceptionfail").send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getValue("success"), equalTo(Boolean.FALSE));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testToNotFail(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/notfailing").send().map(res -> {
			assertThat(res, is(status(new Created())));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
