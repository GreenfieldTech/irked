package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle PATCH requests
 * @author odeda
 */
@Repeatable(Patches.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {

	/**
	 * Path on which to accept the PATCH request
	 * @return path
	 */
	String value();

}
