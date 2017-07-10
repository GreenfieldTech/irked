package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tech.greenfield.vertx.irked.handlers.WebHandler;
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
	WebHandler getHandler() throws IllegalArgumentException, IllegalAccessException {
		if (!method.isAccessible())
			throw new IllegalAccessError("Invalid access permissions for " + this);
		Class<?>[] params = method.getParameterTypes();
		if (params.length == 1 || params[0].isAssignableFrom(Request.class) || 
				// we should support working with methods that take specializations for Request, we'll rely on the specific implementation's
				// getRequest() to provide the correct type
				Request.class.isAssignableFrom(params[0])) 
			return r -> {
				try {
					method.invoke(impl, r);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// shouldn't happen
					throw new InternalServerError("Invalid request handler " + this + ": " + e, e).uncheckedWrap();
				}
			};
		throw new IllegalArgumentException("Invalid arguments for " + this);
	}

}
