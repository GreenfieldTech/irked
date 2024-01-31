package tech.greenfield.vertx.irked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;
import tech.greenfield.vertx.irked.exceptions.InvalidRouteConfiguration;

/**
 * Helper for creating Irked routers
 * @author odeda
 */
public class Irked {

	private Vertx vertx;

	/**
	 * Vert.x-styled Irked factory method.
	 * @param vertx Vert.x instance in which to generated routers
	 * @return a new Irked instance that can generate {@link Router}s and request handlers
	 */
	public static Irked irked(Vertx vertx) {
		return new Irked(vertx);
	}
	
	/**
	 * Create new Irked instance that can generate request handlers
	 * @deprecated Will be removed in 5.0. Please use {@link Irked#irked(Vertx)} instead
	 * @param vertx Vert.x instance in which to generate routers
	 */
	@Deprecated(forRemoval = true)
	public Irked(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * Create an HTTP request handler, that can be used in {@link HttpServer#requestHandler()},
	 * with the provided set of APIs configured
	 * @param apis set of Irked API controllers to configure routing for
	 * @return an HTTP request handler for the Vert.x HTTP server
	 * @throws InvalidRouteConfiguration in case one of the route configurations is invalid
	 */
	public Handler<HttpServerRequest> setupRequestHandler(Controller... apis) throws InvalidRouteConfiguration {
		Router router = new Router(vertx);
		for (Controller api : apis)
			router.configure(api);
		return router;
	}
	
	/**
	 * Create a new Irked router
	 * @return router that can be used to configure Irked API controllers and handle HTTP requests from Vert.x HTTP server
	 */
	public Router router() {
		return new Router(vertx);
	}
	
	/**
	 * Create a new Irked router
	 * @param vertx Vert.x instance in which to generate routers
	 * @return router that can be used to configure Irked API controllers and handle HTTP requests from Vert.x HTTP server
	 */
	public static Router router(Vertx vertx) {
		return new Router(vertx);
	}
	
	/**
	 * Create a {@link LoggerHandler} instance to log HTTP access under a simple SLF4J logger name.
	 * The implementation returned from is a subclass of the original Vert.x {@link LoggerHandlerImpl}
	 * except where the underlying logger is replaced so that the logger isn't named for a full class name
	 * of an internal Vert.x implementation class - this makes it easier to set up filters and manage the
	 * access log.
	 * 
	 * To use - define a field in your main (root) controller like so:
	 * 
	 * <pre><code>
	 * &#x0040;Endpoint private LoggerHandler accessLog = Irked.logger();
	 * </code></pre>
	 * 
	 * This method uses the hard coded log name "{@code access}" and the default log format.
	 * 
	 * @return an instance of a {@code LoggerHandler} that will log HTTP requests
	 */
	public static LoggerHandler logger() {
		return logger("access", LoggerFormat.DEFAULT);
	}
	
	/**
	 * Create a {@link LoggerHandler} instance to log HTTP access under a simple SLF4J logger name.
	 * The implementation returned from is a subclass of the original Vert.x {@link LoggerHandlerImpl}
	 * except where the underlying logger is replaced so that the logger isn't named for a full class name
	 * of an internal Vert.x implementation class - this makes it easier to set up filters and manage the
	 * access log.
	 * 
	 * To use - define a field in your main (root) controller like so:
	 * 
	 * <pre><code>
	 * &#x0040;Endpoint private LoggerHandler accessLog = Irked.logger(LoggerFormat.SHORT);
	 * </code></pre>
	 * 
	 * This method uses the hard coded log name "{@code access}".
	 * 
	 * @param format LoggerFormat to use for formatting the log
	 * @return an instance of a {@code LoggerHandler} that will log HTTP requests
	 */
	public static LoggerHandler logger(LoggerFormat format) {
		return logger("access", format);
	}
	
	/**
	 * Create a {@link LoggerHandler} instance to log HTTP access under a simple SLF4J logger name.
	 * The implementation returned from is a subclass of the original Vert.x {@link LoggerHandlerImpl}
	 * except where the underlying logger is replaced so that the logger isn't named for a full class name
	 * of an internal Vert.x implementation class - this makes it easier to set up filters and manage the
	 * access log.
	 * 
	 * To use - define a field in your main (root) controller like so:
	 * 
	 * <pre><code>
	 * &#x0040;Endpoint private LoggerHandler accessLog = Irked.logger("access_log", LoggerFormat.SHORT);
	 * </code></pre>
	 * 
	 * @param loggerName text to use for the logger name
	 * @param format LoggerFormat to use for formatting the log
	 * @return an instance of a {@code LoggerHandler} that will log HTTP requests
	 */
	public static LoggerHandler logger(String loggerName, LoggerFormat format) {
		return new LoggerHandlerImpl(format) {
			private Logger LOG = LoggerFactory.getLogger(loggerName);
			@Override
			protected void doLog(int status, String message) {
				if (status >= 500) {
					LOG.error(message);
				} else if (status >= 400) {
					LOG.warn(message);
				} else {
					LOG.info(message);
				}
			}
		};
	}
}
