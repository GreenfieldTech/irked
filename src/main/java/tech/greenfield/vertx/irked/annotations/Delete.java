package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle DELETE requests
 * @author odeda
 */
@Repeatable(Deletes.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete {

	/**
	 * Path on which to accept the DELETE request
	 * @return path
	 */
	String value();

}
