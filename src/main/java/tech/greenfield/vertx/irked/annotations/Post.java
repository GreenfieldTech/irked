package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure a route to handle POST requests
 * @author odeda
 */
@Repeatable(Posts.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {

	/**
	 * Path on which to accept the POST request
	 * @return path
	 */
	String value();

}
