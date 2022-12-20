package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Repeatable container for <code>@Post</code>
 * @author odeda
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Posts {
	Post[] value();
}
