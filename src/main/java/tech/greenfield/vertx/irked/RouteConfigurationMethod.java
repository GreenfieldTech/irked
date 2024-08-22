package tech.greenfield.vertx.irked;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouteImplHelper;
import tech.greenfield.vertx.irked.annotations.OnFail;
import tech.greenfield.vertx.irked.annotations.WebSocket;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

/**
 * An implementation of {@link RouteConfiguration} used to configure Vert.x-web for Controller methods
 * @author odeda
 */
public class RouteConfigurationMethod extends RouteConfiguration {
	private final Method method;
	private final Parameter[] params;
	private final Map<String, Function<Request, Object>> paramResolvers = new HashMap<>();

	public RouteConfigurationMethod(Controller impl, Router router, Method m) throws InvalidRouteConfiguration {
		super(impl, router, m.getAnnotations());
		method = m;
		params = m.getParameters();
		if (!isValid())
			return; // don't sanity check methods that aren't routing methods
		//if ((m.getModifiers() & Modifier.PUBLIC) == 0)
		//	throw new InvalidRouteConfiguration("Method " + m.getName() + " is not public");
		if (isWebSocketHandler()) return; // satisfies
		if (params.length < 1 || !RoutingContext.class.isAssignableFrom(params[0].getType()))
			throw new InvalidRouteConfiguration(String.format("Method %1$s.%2$s doesn't take a Vert.x RoutingContext as first parameter",
					m.getDeclaringClass().getName(), m.getName()));
		trySetRoutingContextType(params[0].getType());
		var routeParams = parseRouteParams(uriForAnnotations());
		Optional<String> paramErrors = Stream.of(params).map(p -> tryResolve(p, routeParams)).filter(Objects::nonNull)
				.reduce((a,b) -> a + "; " + b);
		if (paramErrors.isPresent())
			throw new InvalidRouteConfiguration(String.format("Method %1$s.%2$s contains parameters that cannot be resolved: %3$s",
					m.getDeclaringClass().getName(), m.getName(), paramErrors.get()));
	}

	private Set<String> parseRouteParams(String[] possibleURIs) {
		Set<String> total = new HashSet<>();
		for (String uri : possibleURIs)
			total.addAll(new RouteImplHelper(router, uri).listParameters());
		return total;
	}

	private String tryResolve(Parameter p, Set<String> routeParams) {
		if (p.isVarArgs())
			return "VarArgs parameters are not supported";
		if (RoutingContext.class.isAssignableFrom(p.getType())) {
			paramResolvers.put(p.getName(), r -> r);
			return null;
		}
		if (isFailHandler() && Throwable.class.isAssignableFrom(p.getType())) {
			// check that the user has requested one of the failure types that they registered,
			// otherwise its a misconfiguration and we should abort before sending nulls to the handler
			// in runtime error handling
			if (Stream.of(getAnnotation(OnFail.class)).map(f -> f.exception()).filter(Objects::nonNull)
					.noneMatch(p.getType()::isAssignableFrom))
				return String.format("Parameter '%1$s %2$s' on failure handler does not match any @OnFail(exception) registration!",
						p.getType().getSimpleName(), p.getName());
			@SuppressWarnings("unchecked")
			Function<Request,Object> lambda = r -> r.findFailure((Class<? extends Throwable>)p.getType());
			paramResolvers.put(p.getName(), lambda);
			return null;
		}
		String name = null;
		if (routeParams.contains(p.getName()))
			name = p.getName();
		else { // try to find an annotation
			findAnnotation: for (Annotation a : p.getAnnotations()) {
				Class<? extends Annotation> t = a.annotationType();
				if (!t.getSimpleName().startsWith("Name"))
					continue;
				for (Method m : t.getDeclaredMethods()) {
					if (m.getReturnType() != String.class || m.getParameterCount() > 0)
						continue;
					try {
						Object val = m.invoke(a);
						if (routeParams.contains(val)) {
							name = (String)val;
							break findAnnotation;
						}
					} catch (Exception e) {}
				}
			}
		}
		if (name == null) // But I still haven't found what I'm looking for
			return String.format("Cannot associate parameter '%1$s %2$s' with any of the URI parameter(s) %3$s",
					p.getType(), p.getName(), routeParams.toString());
		Function<Request,Object> resolver = null;
		
		// create resolvers - note that all these may trivially fail to null (possibly by throwing NPEs) when there
		// are multiple URIs with non-equivalent path parameter lists.
		final String paramName = name;
		if (p.getType() == String.class)
			resolver = r -> r.pathParam(paramName);
		else if (p.getType() == Boolean.class)
			resolver = r -> { try { return r.pathParam(paramName).toLowerCase().equals("true"); } catch (Exception e) { return null; } };
		else if (p.getType() == Long.class)
			resolver = r -> { try { return Long.parseLong(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else if (p.getType() == Integer.class)
			resolver = r -> { try { return Integer.parseInt(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else if (p.getType() == Float.class)
			resolver = r -> { try { return Float.parseFloat(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else if (p.getType() == Double.class)
			resolver = r -> { try { return Double.parseDouble(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else if (p.getType() == BigDecimal.class)
			resolver = r -> { try { return new BigDecimal(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else if (p.getType() == Instant.class)
			resolver = r -> { try { return Instant.parse(r.pathParam(paramName)); } catch (Exception e) { return null; } };
		else
			return String.format("Type '%1$s' (for parameter '%2$s') is not supported", p.getType(), p.getName());
		paramResolvers.put(p.getName(), resolver);
		return null;
	}
	
	private boolean isWebSocketHandler() throws InvalidRouteConfiguration {
		Map<Boolean, List<Annotation>> types = Arrays.stream(annotations).collect(Collectors.partitioningBy((Annotation a) -> a.annotationType().equals(WebSocket.class)));
		if (types.get(false).size() > 0 && types.get(true).size() > 0)
			throw new InvalidRouteConfiguration("A WebSocket handler " + method + " cannot also be a request handler");
		if (types.get(false).size() > 0)
			return false;
		if (params.length == 1 && WebSocketMessage.class.isAssignableFrom(params[0].getType()))
			return true;
		if (params.length == 2 && Request.class.isAssignableFrom(routingContextType) && WebSocketMessage.class.isAssignableFrom(params[1].getType()))
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
	Handler<? super Request> getHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		method.setAccessible(true);
		return new Handler<Request>() {
			@Override
			public void handle(Request r) {
				try {
					method.invoke(impl, createParamBlock(resolveRequestContext(r)));
				} catch (RoutingContextImplException e) {
					r.fail(new InternalServerError(e.getMessage(), e));
				} catch (InvocationTargetException e) { // user exception
					handleUserException(r, e.getCause(), "method " + method);
				} catch (IllegalAccessException e) { // shouldn't happen because we setAccessible above
					r.fail(new InternalServerError("Invalid request handler " + this + ": " + e, e));
				} catch (IllegalArgumentException e) { // shouldn't happen because we checked the type before calling
					r.fail(new InternalServerError("Mistyped request handler " + this + ": " + e, e));
				}
			}
			@Override
			public String toString() {
				return method.getName() + "(" + Arrays.asList(method.getParameterTypes()).stream().map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")";
			}
		};
	}

	@Override
	Handler<? super WebSocketMessage> getMessageHandler() throws IllegalArgumentException, IllegalAccessException, InvalidRouteConfiguration {
		method.setAccessible(true);
		return new Handler<WebSocketMessage>() {
			@Override
			public void handle(WebSocketMessage m) {
				Request req = m.request();
				// run time check for correct type
				// we support working with methods that take specializations for Request, we'll rely on the specific implementation's
				// getRequest() to provide the correct type
				if (!routingContextType.isAssignableFrom(m.getClass()) && !routingContextType.isAssignableFrom(req.getClass())) {
					req.fail(new InternalServerError("Invalid request handler " + this + " - can't handle request of type " + req.getClass()));
					return;
				}
				
				try {
					// we do not (ATM) support dynamic parameter parsing on websocket message handlers
					if (params.length == 1)
						method.invoke(impl, m);
					else
						method.invoke(impl, req, m);
				} catch (InvocationTargetException e) { // user exception
					handleUserException(m, e.getCause(), "method " + method);
				} catch (IllegalAccessException | IllegalArgumentException e) {
					// shouldn't happen because we setAccessible above and we checked the type before calling
					handleUserException(m, e, "method " + method);
				}
			}
			@Override
			public String toString() {
				return method.getName() + "(" + Arrays.asList(method.getParameterTypes()).stream().map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")";
			}
		};
	}

	private Object[] createParamBlock(Request r) {
		Object[] invokeParams = new Object[params.length];
		invokeParams[0] = r;
		for (int i = 1; i < params.length; i++)
			invokeParams[i] = paramResolvers.computeIfAbsent(params[i].getName(), (req -> null)).apply(r);
		return invokeParams;
	}
	
}
