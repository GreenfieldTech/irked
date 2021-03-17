package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public class RouteConfigurationField extends RouteConfiguration {

	private Field field;

	public RouteConfigurationField(Controller impl, Field f) {
		super(impl, f.getAnnotations());
		field = f;
	}

	@Override
	protected <T extends Annotation> T[] getAnnotation(Class<T> anot) {
		return field.getDeclaredAnnotationsByType(anot);
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
		final Handler<RoutingContext> handler = (Handler<RoutingContext>)field.get(impl);
		return new Handler<RoutingContext>() {
			@Override
			public void handle(RoutingContext r) {
				try {
					handler.handle(r);
				} catch (Throwable cause) {
					handleUserException(r, cause, "field " + field);
				}
			}
			@Override
			public String toString() {
				return field.getName();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	Handler<? super WebSocketMessage> getMessageHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		if (!Handler.class.isAssignableFrom(field.getType()))
			throw new InvalidRouteConfiguration(this + " is not a valid handler");
		field.setAccessible(true);
		final Handler<WebSocketMessage> handler = (Handler<WebSocketMessage>)field.get(impl);
		return new Handler<WebSocketMessage>() {
			@Override
			public void handle(WebSocketMessage m) {
				try {
					handler.handle(m);
				} catch (Throwable cause) {
					handleUserException(m, cause, "field " + field);
				}
			}
			@Override
			public String toString() {
				return field.getName();
			}
		};
	}

}
