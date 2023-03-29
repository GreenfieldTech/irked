package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle PUT requests
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * PUT requests.
 * @author odeda
 */
@Repeatable(Puts.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Put {

	/**
	 * Path on which to accept the PUT request
	 * @return path
	 */
	String value() default "/*";

}
