package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle all HTTP requests
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * requests.
 * @author odeda
 */
@RouteSpec
@Repeatable(Endpoints.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Endpoint {

	/**
	 * Path on which to handle all HTTP requests
	 * @return path
	 */
	String value() default "/*";

}
