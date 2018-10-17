package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.HttpError.UncheckedHttpError;
import tech.greenfield.vertx.irked.annotations.WebSocket;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public class RouteConfigurationMethod extends RouteConfiguration {

	private Method method;
	private Class<?>[] params;

	public RouteConfigurationMethod(Controller impl, Method m) throws InvalidRouteConfiguration {
		super(impl, m.getAnnotations());
		method = m;
		if (!isValid())
			return; // don't sanity check methods that aren't routing methods
		//if ((m.getModifiers() & Modifier.PUBLIC) == 0)
		//	throw new InvalidRouteConfiguration("Method " + m.getName() + " is not public");
		params = m.getParameterTypes();
		if (isWebSocketHandler()) return; // satisfies
		if (params.length < 1 || !RoutingContext.class.isAssignableFrom(params[0]))
			throw new InvalidRouteConfiguration("Method " + m.getName() + " doesn't take a Vert.x RoutingContext as first parameter");
		if (m.getParameterCount() > 1)
			throw new InvalidRouteConfiguration("Method " + m.getName() + " requires more than one parameter which I don't know how to provide yet");
	}

	private boolean isWebSocketHandler() throws InvalidRouteConfiguration {
		Map<Boolean, List<Annotation>> types = Arrays.stream(annotations).collect(Collectors.partitioningBy((Annotation a) -> a.annotationType().equals(WebSocket.class)));
		if (types.get(false).size() > 0 && types.get(true).size() > 0)
			throw new InvalidRouteConfiguration("A WebSocket handler " + method + " cannot also be a request handler");
		if (types.get(false).size() > 0)
			return false;
		if (params.length == 1 && WebSocketMessage.class.isAssignableFrom(params[0]))
			return true;
		if (params.length == 2 && Request.class.isAssignableFrom(params[0]) && WebSocketMessage.class.isAssignableFrom(params[1]))
			return true;
		throw new InvalidRouteConfiguration("A WebSocket handler " + method + " must accept (WebSocketMessage) or (Request,WebSocketMessage)");
	}
	
	@Override
	protected <T extends Annotation> T[] getAnnotation(Class<T> anot) {
		return method.getDeclaredAnnotationsByType(anot);
	}
	
	@Override
	public boolean isController() {
		return false; // a method is always a request handler and never a sub router
	}

	@Override
	Controller getController() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	protected String getName() {
		return method.getName();
	}

	@Override
	Handler<? super RoutingContext> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		method.setAccessible(true);
		return r -> {
			// run time check for correct type
			// we support working with methods that take specializations for Request, we'll rely on the specific implementation's
			// getRequest() to provide the correct type
			if (!params[0].isAssignableFrom(r.getClass())) {
				r.fail(new InternalServerError("Invalid request handler " + this + " - can't handle request of type " + r.getClass()));
				return;
			}
			
			try {
				method.invoke(impl, r);
			} catch (InvocationTargetException e) { // user exception
				if (r.failed()) {
					log.warn("Exception occured on a fail route, ignoring",e);
					return;
				}
				Throwable cause = e.getCause();
				if (cause instanceof UncheckedHttpError || cause instanceof HttpError)
					r.fail(HttpError.toHttpError(cause));
				else {
					log.error("Handler method " + method + " threw an unexpected exception",cause);
					r.fail(cause); // propagate exceptions thrown by the method to the Vert.x fail handler
				}
			} catch (IllegalAccessException e) { // shouldn't happen because we setAccessible above
				r.fail(new InternalServerError("Invalid request handler " + this + ": " + e, e));
			} catch (IllegalArgumentException e) { // shouldn't happen because we checked the type before calling
				r.fail(new InternalServerError("Mistyped request handler " + this + ": " + e, e));
			}
		};
	}

	@Override
	Handler<? super WebSocketMessage> getMessageHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		method.setAccessible(true);
		return m -> {
			Request req = m.request();
			// run time check for correct type
			// we support working with methods that take specializations for Request, we'll rely on the specific implementation's
			// getRequest() to provide the correct type
			if (!params[0].isAssignableFrom(m.getClass()) && !params[0].isAssignableFrom(req.getClass())) {
				req.fail(new InternalServerError("Invalid request handler " + this + " - can't handle request of type " + req.getClass()));
				return;
			}
			
			try {
				if (params.length == 1)
					method.invoke(impl, m);
				else
					method.invoke(impl, req, m);
			} catch (InvocationTargetException e) { // user exception
				m.request().fail(e.getCause()); // propagate exceptions thrown by the method to the Vert.x fail handler
			} catch (IllegalAccessException e) { // shouldn't happen because we setAccessible above
				m.request().fail(new InternalServerError("Invalid request handler " + this + ": " + e, e));
			} catch (IllegalArgumentException e) { // shouldn't happen because we checked the type before calling
				m.request().fail(new InternalServerError("Mistyped request handler " + this + ": " + e, e));
			}
		};
	}

}
