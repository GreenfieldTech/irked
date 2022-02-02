package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle HEAD requests
 * @author odeda
 */
@Repeatable(Heads.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Head {

	/**
	 * Path on which to handle HEAD requests
	 * @return path
	 */
	String value();

}
