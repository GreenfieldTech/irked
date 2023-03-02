package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle CONNECT requests.
 * Set the value to configure for a specific (possibly wild-card) path, or leave empty to handle all
 * CONNECT requests.
 * @author odeda
 */
@Repeatable(ConnectSpecs.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Connect {

	/**
	 * Path on which to accept the CONNECT request.
	 * @return path
	 */
	String value() default "/*";

}
