package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle PATCH requests
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * PATCH requests.
 * @author odeda
 */
@RouteSpec
@Repeatable(Patches.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {

	/**
	 * Path on which to accept the PATCH request
	 * @return path
	 */
	String value() default "/*";

}
