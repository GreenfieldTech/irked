package tech.greenfield.vertx.irked.example;

import io.vertx.core.json.JsonObject;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.annotations.Endpoint;

/**
 * Example controller for Irked example application
 */
public class ExampleAPIv1 extends Controller {

	@Endpoint("/")
	WebHandler start = r -> r.sendJSON(new JsonObject().put("version", 1)); 

}
