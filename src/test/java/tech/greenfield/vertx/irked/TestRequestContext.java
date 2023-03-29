package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.Get;
import tech.greenfield.vertx.irked.annotations.Put;
import tech.greenfield.vertx.irked.base.TestBase;

public class TestRequestContext extends TestBase {

	public class IdContext extends Request {

		private String id;

		public IdContext(Request req, String currentId) {
			super(req);
			id = currentId;
		}
		
		public String getId() {
			return id;
		}

	}

	public class TestController extends Controller {
		
		@Get("/:id")
		public void retrieve(IdContext r) {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		}

		@Put("/:id")
		Handler<IdContext> update = r -> {
			r.sendJSON(new JsonObject().put("id", r.getId()));
		};

		@Override
		protected Request getRequestContext(Request req) {
			String currentId = req.pathParam("id");
			if (Objects.isNull(currentId))
				return req;
			return new IdContext(req, currentId);
		}
		
	}

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testPassIdToMethod(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		String id = "item-name";
		getClient(vertx).get(port, "localhost", "/" + id).send().map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), equalTo(id));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

	@Test
	public void testPassIdToField(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		String id = "item-name";
		getClient(vertx).put(port, "localhost", "/" + id).send("{}").map(res -> {
			assertThat(res, isSuccess());
			JsonObject o = res.bodyAsJsonObject();
			assertThat(o.getString("id"), equalTo(id));
			return null;
		})
		.onFailure(context::failNow)
		.onSuccess(flag(async));
	}

}
