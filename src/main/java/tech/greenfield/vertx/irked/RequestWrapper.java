package tech.greenfield.vertx.irked;

import java.util.Objects;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.OrderListener;

public class RequestWrapper implements Function<RoutingContext, Request>, Handler<RoutingContext>, OrderListener {
	
	private enum Type { Root, Controller, Handler, Custom }

	private Type type;
	private Controller ctr;
	protected Function<RoutingContext, Request> wrapper;
	private Handler<? super RoutingContext> handler;
	
	public RequestWrapper(Controller ctr) {
		this(ctr, Request::new);
		type = Type.Root;
	}

	public RequestWrapper(Controller ctr, Function<RoutingContext, Request> requestWrapper) {
		this.ctr = Objects.requireNonNull(ctr, "Controller instance is not set!");
		this.wrapper = requestWrapper;
		type = Type.Controller;
	}
	
	public RequestWrapper(Handler<? super RoutingContext> handler, Function<RoutingContext, Request> requestWrapper) {
		this.handler = Objects.requireNonNull(handler, "Handler instance is not set!");
		this.wrapper = requestWrapper;
		type = Type.Handler;
	}
	
	/**
	 * Helper c'tor for extensions that want to provide their own handling
	 * @param parent Wrapping wrapper
	 */
	protected RequestWrapper(Function<RoutingContext, Request> parent) {
		wrapper = parent;
		type = Type.Custom;
	}

	@Override
	public Request apply(RoutingContext r) {
		return ctr.getRequestContext(wrapper.apply(r));
	}

	@Override
	public void handle(RoutingContext r) {
		this.handler.handle(wrapper.apply(r));
	}

	public String toString() {
		switch (type) {
		case Root: return "HTTP=>" + ctr;
		case Controller: return wrapper + "->" + ctr;
		case Handler: return wrapper + "." + handler;
		default:
		case Custom: return wrapper + "->?";
		}
	}

	/**
	 * This method is only implemented to support root level Vert.x handler that rely on the onOrder()
	 * method to be called (e.g. OAuth2AuthHandler) to get that call, otherwise ordering for Irked controllers
	 * is handled by implicit field ordering in the java class source code file.
	 */
	@Override
	public void onOrder(int order) {
		switch (type) {
		case Handler:
			if (this.handler instanceof OrderListener)
				((OrderListener)this.handler).onOrder(order);
			break;
		case Custom:
			break; // custom wrappers can't get onOrder()
		default:
			// internal controllers' handlers are expected to maintain order implicit field order
			break;
		}
	}
}