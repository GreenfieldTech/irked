package tech.greenfield.vertx.irked.base;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.server.Server;

@RunWith(VertxUnitRunner.class)
@Ignore public class TestBase {

	@ClassRule
	public static RunTestOnContext rule = new RunTestOnContext();
	@Rule
	public Timeout timeoutRule = Timeout.seconds(3600);
	protected final Integer port = 1234;

	protected HttpClient getClient() {
		return rule.vertx().createHttpClient(new HttpClientOptions().setIdleTimeout(0));
	}

	protected void deployController(Controller controller, Handler<AsyncResult<String>> handler) {
		Server server = new Server(controller);
	
		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		rule.vertx().deployVerticle(server, options, handler);
	}

}
