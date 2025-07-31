package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.function.Function;

import io.vertx.core.Future;
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
		var type = field.getGenericType();
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType)type;
			var rtype = pType.getActualTypeArguments()[0];
			try {
				trySetRoutingContextType(Class.forName(rtype.getTypeName()));
			} catch (ClassNotFoundException e) {
			}
		}
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
	
	protected void configureRoute(RequestWrapper requestWrapper, io.vertx.ext.web.Route r) throws InvalidRouteConfiguration {
		if (io.vertx.ext.web.Router.class.isAssignableFrom(field.getType()))
			try {
				var router = (io.vertx.ext.web.Router) field.get(impl);
				if (router == null)
					throw new InvalidRouteConfiguration(this + " is not set!");
				r.subRouter(router);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new InvalidRouteConfiguration("Error configuring subrouter on " + this + ": " + e);
			}
		else
			super.configureRoute(requestWrapper, r);
	};
	
	@Override
	Handler<? super Request> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		if (Handler.class.isAssignableFrom(field.getType()))
			return getFieldHandler();
		if (Function.class.isAssignableFrom(field.getType()))
			return getFunctionHandler();
		throw new InvalidRouteConfiguration(this + " is not a valid handler or controller");
	}
	
	Handler<? super Request> getFieldHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		final Handler<Request> handler = (Handler<Request>) field.get(impl);
		if (handler instanceof OrderListener)
			return new FieldHandlerWithOrderListener(handler);
		return new FieldHandler(handler);
	}
	
	Handler<? super Request> getFunctionHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		final Function<Request, Object> handler = (Function<Request,Object>) field.get(impl);
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
		public FieldHandler(Function<Request, Object> handler) {
			this.handler = r -> {
				try {
					Object retValue = handler.apply(r);
					Future<?> resultHandler = retValue instanceof Future ? (Future<?>) retValue : Future.succeededFuture(retValue);
					resultHandler.onComplete(val -> {
						if (!r.response().headWritten())
							r.sendOrFail(val);
					});
				} catch (Throwable cause) {
					r.fail(cause);
				}
			};
		}
		@Override
		public void handle(Request r) {
			try {
				handler.handle(resolveRequestContext(r));
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

		public FieldHandlerWithOrderListener(Function<Request, Object> handler) {
			super(handler);
		}

		@Override
		public void onOrder(int order) {
			((OrderListener)handler).onOrder(order);
		}
		
	}

}
