package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.vertx.ext.web.Route;

/**
 * Annotation to configure an explicit "order" for method handlers.
 * This annotation exposes the Vert.x {@link Route#order(int)} API as an
 * an alternative way to force handler order, if you don't like
 * to write lambda handler fields, or handler reference fields.
 * 
 * Note that because Vert.x-web internally sets an incrementing (0-based) order for handlers
 * set up without an explicit order specified, mixing handlers with {@code @Order}
 * and without can lead to undesired results. One strategy to mitigate this is to use
 * the {@code @Order} annotation only with very large numbers (1000 and above) to make
 * sure explicitly ordered handlers are executed after all implicitly ordered handlers,
 * or to set negative numbers to make sure explicitly ordered handlers are executed before
 * all implicitly ordered handlers. 
 * 
 * @see <a href="https://vertx.io/docs/vertx-web/java/#_route_order">Vert.x-web ordering documentation</a>
 * @author Oded Arbel <oded@geek.co.il>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

	/**
	 * The order (ascending) that this handler will be called within all handlers
	 * that match the current requests.
	 * @return order
	 */
	int value();
}
