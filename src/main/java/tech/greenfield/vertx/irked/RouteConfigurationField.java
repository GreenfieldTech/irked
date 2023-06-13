package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import io.vertx.core.Handler;
import io.vertx.ext.web.impl.OrderListener;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

/**
 * An implementation of {@link RouteConfiguration} used to configure Vert.x-web for Controller fields
 * @author odeda
 */
public class RouteConfigurationField extends RouteConfiguration {

	private Field field;

	public RouteConfigurationField(Controller impl, Router router, Field f) {
		super(impl, router, f.getAnnotations());
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
	public Controller getController() {
		return impl.getController(field);
	}

	@Override
	protected String getName() {
		return field.getName();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	Handler<? super Request> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		if (!Handler.class.isAssignableFrom(field.getType()))
			throw new InvalidRouteConfiguration(this + " is not a valid handler or controller");
		field.setAccessible(true);
		final Handler<Request> handler = (Handler<Request>) field.get(impl);
		if (handler instanceof OrderListener)
			return new FieldHandlerWithOrderListener(handler);
		return new FieldHandler(handler);
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

	private class FieldHandler implements Handler<Request> {
		protected Handler<Request> handler;
		public FieldHandler(Handler<Request> handler) {
			this.handler = handler;
		}
		@Override
		public void handle(Request r) {
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
	
	private class FieldHandlerWithOrderListener extends FieldHandler implements OrderListener {

		public FieldHandlerWithOrderListener(Handler<Request> handler) {
			super(handler);
		}

		@Override
		public void onOrder(int order) {
			((OrderListener)handler).onOrder(order);
		}
		
	}

}
