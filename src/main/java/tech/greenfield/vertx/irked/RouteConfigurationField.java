package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

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

	@SuppressWarnings("unchecked")
	@Override
	Handler<? super RoutingContext> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		if (!Handler.class.isAssignableFrom(field.getType()))
			throw new InvalidRouteConfiguration(this + " is not a valid handler or controller");
		field.setAccessible(true);
		return (Handler<RoutingContext>)field.get(impl);
	}

}
