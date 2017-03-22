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

	public Request(RoutingContext outerContext) {
		super(outerContext.currentRoute(), outerContext);
	}

}
