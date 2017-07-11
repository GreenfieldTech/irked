package tech.greenfield.vertx.irked;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextDecorator;
import tech.greenfield.vertx.irked.status.OK;

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
	
	/**
	 * Helper method to terminate request processing with a success (200 OK) response containing a JSON body.
	 * @param json {@link JsonObject} containing the output to encode
	 */
	public void sendJSON(JsonObject json) {
		sendJSON(json, new OK());
	}
	
	/**
	 * Helper method to terminate request processing with a custom response containing a JSON body and the specified
	 * status line.
	 * @param json {@link JsonObject} containing the output to encode
	 * @param status HTTP status to send
	 */
	public void sendJSON(JsonObject json, HttpError status) {
		String content = json.encode();
		response(status)
				.putHeader("Content-Type", "application/json")
				.putHeader("Content-Length", String.valueOf(content.length()))
				.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP error (non-200 OK) response.
	 * The resulting HTTP response will have the correct status line and an application/json content
	 * with a JSON encoded object containing the fields "status" set to "false" and "message" set
	 * to the {@link HttpError}'s message.
	 * @param err
	 */
	public void sendError(HttpError err) {
		sendJSON(new JsonObject().put("status", false).put("message", err.getMessage()), err);
	}

	/**
	 * Helper method to generate response with the specified HTTP status
	 * @param status HTTP status code and text to set on the response
	 * @return HTTP response created using {@link RoutingContext#response()}
	 */
	public HttpServerResponse response(HttpError status) {
		return response().setStatusCode(status.getStatusCode()).setStatusMessage(status.getStatusText());
	}

}