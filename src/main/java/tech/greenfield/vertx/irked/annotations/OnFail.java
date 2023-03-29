package tech.greenfield.vertx.irked.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

/**
 * Annotation to configure a route to handle requests that have failed
 * @author odeda
 */
@Repeatable(OnFailures.class)
@Retention(RUNTIME)
public @interface OnFail {
	int status() default -1;
	Class<? extends Throwable> exception() default Throwable.class;
}
