package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.vertx.ext.web.handler.TimeoutHandler;

/**
 * Annotation that will add a {@link TimeoutHandler} to the configured route, causing that route
 * to fail with a 503 server error if no response was written after the specified timeout.
 * @author odeda
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {

	/**
	 * Timeout in milliseconds to configure for the {@link TimeoutHandler}
	 * @return number of milliseconds to wait for a response before returning a 503 server error
	 */
	int value();
	
}
