package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle GET requests
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * GET requests.
 * @author odeda
 */
@RouteSpec
@Repeatable(Gets.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Get {

	/**
	 * Path on which to handle GET request
	 * @return path
	 */
	String value() default "/*";

}
