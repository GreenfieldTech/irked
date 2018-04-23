package tech.greenfield.vertx.irked.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

@Repeatable(WebSockets.class)
@Retention(RUNTIME)
public @interface WebSocket {

	String value();
	
}
