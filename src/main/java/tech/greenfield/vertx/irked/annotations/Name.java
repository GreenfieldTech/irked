package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A name annotation useful for dynamic parameter passing.
 * This is annotation is not a requirement for dynamic parameter passing but instead provided as an optional extra
 * for uses that do already have a robust naming annotation in use (such as `javax.inject.Named`).
 * @author odeda
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {
	String value();
}
