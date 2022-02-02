package tech.greenfield.vertx.irked.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to configure a route to handle requests that have failed
 * @author odeda
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface OnFail {

}
