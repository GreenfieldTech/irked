package tech.greenfield.vertx.irked.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to label a route as "blocking"
 * i.e. it will be run in a context that is allowed to block for long periods of time
 * @author odeda *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Blocking {

}
