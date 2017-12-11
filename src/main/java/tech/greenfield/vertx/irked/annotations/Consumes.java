package tech.greenfield.vertx.irked.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(ConsumesSpecs.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Consumes {

	String value();

}
