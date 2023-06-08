package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.math.BigDecimal;
import java.time.Instant;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Name;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestDynamicMethodParameters extends TestBase {

	class TestContext extends Request {

		public TestContext(Request req) {
			super(req);
		}
		
	}
	
	public class TestController extends Controller {
		
		@Get("/int/:id")
		public void readInt(RoutingContext r, @Name("id") Integer id) {
			r.response().end("int=" + id);
		}

		@Get("/long/:val")
		public void readLong(TestContext r, @Named("val") Long val) {
			r.send("long=" + val);
		};

		@Get("/text/:text")
		public void readString(Request r, @Name("text") String text) {
			r.send("text=" + text);
		};

		@Get("/boolean/:val")
		public void readBoolean(Request r, @Named("val") Boolean val) {
			r.send("boolean=" + val);
		};

		@Get("/time/:val")
		public void readTime(Request r, @Named("val") Instant val) {
			r.send("time=" + val);
		};

		@Get("/float/:val")
		public void readFloat(Request r, @Named("val") Float val) {
			r.send("float=" + val);
		};

		@Get("/double/:val")
		public void readDouble(Request r, @Named("val") Double val) {
			r.send("double=" + val);
		};

		@Get("/decimal/:val")
		public void readString(Request r, @Named("val") BigDecimal val) {
			r.send("decimal=" + val);
		};

		@Override
		protected Request getRequestContext(Request req) {
			return new TestContext(req);
		}
		
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testParseInt(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/int/5").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("int=5")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseLong(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/long/435907809").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("long=435907809")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseString(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/text/foo").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("text=foo")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseBoolean(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/boolean/no").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("boolean=false")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseTime(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/time/2023-06-08T22:29:03Z").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("time=2023-06-08T22:29:03Z")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseFloat(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/float/4.258").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("float=4.258")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseDouble(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/double/108065890521.125").send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("double=1.08065890521125E11")));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testParseDecimal(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		String decimal = "306923168211298138556082187122716936545933260919336609116099126682592457995782122841777287586234996123871525212711783944512732025692188753622394246851343725684967749652943423379328211942134672604152722708328447378174332920118072977223834238729819646116898139559732134341417832193236651908016351216431978831917866826112042179015286193152388222874715110730214832111195323160411393112233509888725332210265356335423828158622171136354406562529161350432208157653194";
		getClient(vertx).get(port, "localhost", "/decimal/" + decimal).send().map(res -> {
			assertThat(res, isSuccess());
			assertThat(res.bodyAsString(), is(equalTo("decimal=" + decimal)));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
