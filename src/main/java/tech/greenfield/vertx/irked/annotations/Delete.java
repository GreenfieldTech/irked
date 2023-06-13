package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle DELETE requests
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * DELETE requests.
 * @author odeda
 */
@RouteSpec
@Repeatable(Deletes.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete {

	/**
	 * Path on which to accept the DELETE request
	 * @return path
	 */
	String value() default "/*";

}
