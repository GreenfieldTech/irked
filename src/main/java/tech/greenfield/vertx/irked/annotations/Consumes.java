package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to configure Route.consumes()
 * @author odeda
 */
@Repeatable(ConsumesSpecs.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Consumes {

	/**
	 * Content-Type for the annotated route
	 * @return Content-Type value
	 */
	String value();

}
