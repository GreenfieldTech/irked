package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Repeatable container for <code>@Consumes</code>
 * @author odeda
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumesSpecs {
	Consumes[] value();
}
