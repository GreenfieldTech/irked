package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.status.InternalServerError;

public class RouteConfigurationMethod extends RouteConfiguration {

	private Method method;

	public RouteConfigurationMethod(Controller impl, Method m) {
		super(impl, m.getAnnotations());
		method = m;
	}

	@Override
	protected <T extends Annotation> T getAnnotation(Class<T> anot) {
		return method.getAnnotation(anot);
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
		Class<?>[] params = method.getParameterTypes();
		if (params.length == 1 || params[0].isAssignableFrom(Request.class) || 
				// we should support working with methods that take specializations for Request, we'll rely on the specific implementation's
				// getRequest() to provide the correct type
				Request.class.isAssignableFrom(params[0])) 
			return r -> {
				try {
					method.invoke(impl, r);
				} catch (InvocationTargetException e) {
					r.fail(e.getCause());
				} catch (IllegalAccessException | IllegalArgumentException e) {
					// shouldn't happen
					throw new InternalServerError("Invalid request handler " + this + ": " + e, e).uncheckedWrap();
				}
			};
		throw new InvalidRouteConfiguration("Invalid arguments list for " + this);
	}

}
