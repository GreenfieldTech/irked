package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle OPTIONS requests
 * @author odeda
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Options {

	/**
	 * Path on which to handle OPTIONS requests
	 * @return path
	 */
	String value();

}
