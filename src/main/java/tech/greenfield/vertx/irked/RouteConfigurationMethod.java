package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
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
		if (params.length < 1 || !RoutingContext.class.isAssignableFrom(params[0]))
			throw new InvalidRouteConfiguration("Method " + m.getName() + " doesn't take a Vert.x RoutingContext as first parameter");
		if (m.getParameterCount() > 1)
			throw new InvalidRouteConfiguration("Method " + m.getName() + " requires more than one parameter which I don't know how to provide yet");
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
				r.fail(e.getCause()); // propagate exceptions thrown by the method to the Vert.x fail handler
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
			// run time check for correct type
			// we support working with methods that take specializations for Request, we'll rely on the specific implementation's
			// getRequest() to provide the correct type
			if (!params[0].isAssignableFrom(m.getClass())) {
				m.request().fail(new InternalServerError("Invalid request handler " + this + " - can't handle request of type " + m.getClass()));
				return;
			}
			
			try {
				method.invoke(impl, m);
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
