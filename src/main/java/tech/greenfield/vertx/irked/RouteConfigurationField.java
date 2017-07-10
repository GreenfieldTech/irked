package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import tech.greenfield.vertx.irked.handlers.WebHandler;

public class RouteConfigurationField extends RouteConfiguration {

	private Field field;

	public RouteConfigurationField(Controller impl, Field f) {
		super(impl, f.getAnnotations());
		field = f;
	}

	@Override
	protected <T extends Annotation> T getAnnotation(Class<T> anot) {
		return field.getAnnotation(anot);
	}

	@Override
	public boolean isController() {
		return Controller.class.isAssignableFrom(field.getType());
	}

	@Override
	Controller getController() {
		return impl.getController(field);
	}

	@Override
	protected String getName() {
		return field.getName();
	}

	@Override
	WebHandler getHandler() throws IllegalArgumentException, IllegalAccessException {
		return (WebHandler) field.get(impl);
	}

}
