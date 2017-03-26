package tech.greenfield.vertx.irked;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextDecorator;

/**
 * Request handling wrapper which adds some useful routines for
 * API writers.
 * 
 * Can serve as a basis for local context parsers that API writers
 * can use to expose path arguments from parent prefixes
 * 
 * @author odeda
 */
public class Request extends RoutingContextDecorator {

	private RoutingContext outerContext;

	public Request(RoutingContext outerContext) {
		super(outerContext.currentRoute(), outerContext);
		this.outerContext = outerContext;
	}

	@Override
	public void fail(int statusCode) { 
		// we're overriding the fail handlers, which for some reason the decorator 
		// feels should be moved to another thread. Instead, use the outer implementation
		// and let it do what's right
		this.outerContext.fail(statusCode);
	}

	@Override
	public void fail(Throwable throwable) {
		// we're overriding the fail handlers, which for some reason the decorator 
		// feels should be moved to another thread. Instead, use the outer implementation
		// and let it do what's right
		this.outerContext.fail(throwable);
	}

}
