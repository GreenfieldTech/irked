package tech.greenfield.vertx.irked;

import java.util.Objects;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RequestWrapper implements Function<RoutingContext, Request>, Handler<RoutingContext> {

	private Controller ctr;
	protected Function<RoutingContext, Request> wrapper;
	private Handler<? super RoutingContext> handler;

	public RequestWrapper(Controller ctr, Function<RoutingContext, Request> requestWrapper) {
		this.ctr = Objects.requireNonNull(ctr, "Controller instance is not set!");
		this.wrapper = requestWrapper;
	}

	public RequestWrapper(Handler<? super RoutingContext> handler, Function<RoutingContext, Request> requestWrapper) {
		this.handler = Objects.requireNonNull(handler, "Handler instance is not set!");
		this.wrapper = requestWrapper;
	}
	
	/**
	 * Helper c'tor for extensions that want to provide their own handling
	 * @param parent Wrapping wrapper
	 */
	protected RequestWrapper(Function<RoutingContext, Request> parent) {
		wrapper = parent;
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
		return "[" + (Objects.nonNull(ctr) ? "Controller:"+ctr : "Handler:"+handler) + (Objects.nonNull(wrapper) ? "->" + wrapper : "") + "]"; 
	}
}