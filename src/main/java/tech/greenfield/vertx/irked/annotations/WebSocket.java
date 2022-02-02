package tech.greenfield.vertx.irked.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

/**
 * Annotation to configure a route to handle websocket requests
 * @author odeda
 */
@Repeatable(WebSockets.class)
@Retention(RUNTIME)
public @interface WebSocket {

	/**
	 * Path on which to accept the websocket request
	 * @return path
	 */
	String value();
	
}
