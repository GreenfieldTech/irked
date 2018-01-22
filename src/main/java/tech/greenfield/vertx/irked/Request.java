package tech.greenfield.vertx.irked;

import java.util.*;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.stomp.utils.Headers;
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
	 * Helper failure handler for CompletableFuture users.
	 * Use at the end of an async chain to succinctly propagate exceptions, as
	 * thus: <code>.exceptionally(req::handleFailure)</code>.
	 * This method will call {@link #fail(Throwable)} after unwrapping
	 * {@link RuntimeException}s as needed.
	 * @param throwable A {@link Throwable} error to fail on
	 * @return null
	 */
	public Void handleFailure(Throwable throwable) {
		fail(HttpError.unwrap(throwable));
		return null;
	}
	
	/**
	 * Helper failure handler for CompletableFuture users.
	 * Use in the middle an async chain to succinctly propagate exceptions, or
	 * success values as thus: <code>.whenComplete(req::handlePossibleFailure)</code>.
	 * This method will call {@link Request#fail(Throwable)} if a failure occured,
	 * after unwrapping {@link RuntimeException}s as needed. It will also pass on
	 * the success value (or null if there was a failure) for the next async
	 * element. Subsequent code can check whether a failure was propagated
	 * by calling {@link #failed()}
	 * @param throwable A {@link Throwable} error to fail on
	 * @return null
	 */
	public <V> V handlePossibleFailure(V successValue, Throwable throwable) {
		if (Objects.nonNull(throwable))
			fail(HttpError.unwrap(throwable));
		return successValue;
	}
	
	/**
	 * Helper method to terminate request processing with a success (200 OK) response 
	 * containing a JSON body.
	 * @param json {@link JsonObject} containing the output to encode
	 */
	public void sendJSON(JsonObject json) {
		sendJSON(json, new OK());
	}
	
	/**
	 * Helper method to terminate request processing with a success (200 OK) response 
	 * containing a JSON body.
	 * @param json {@link JsonArray} containing the output to encode
	 */
	public void sendJSON(JsonArray json) {
		sendJSON(json, new OK());
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonObject} containing the output to encode
	 * @param status HTTP status to send
	 */
	public void sendJSON(JsonObject json, HttpError status) {
		sendContent(json.encode(), status, "application/json");
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonObject} containing the output to encode
	 * @param status HTTP status to send
	 * @param headers
	 */
	public void sendJSON(JsonObject json, HttpError status, Headers headers) {
		sendContent(json.encode(), status, "application/json", headers);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonArray} containing the output to encode
	 * @param status HTTP status to send
	 */
	public void sendJSON(JsonArray json, HttpError status) {
		sendContent(json.encode(), status, "application/json");
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonArray} containing the output to encode
	 * @param status HTTP status to send
	 * @param headers
	 */
	public void sendJSON(JsonArray json, HttpError status, Headers headers) {
		sendContent(json.encode(), status, "application/json", headers);
	}
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content
	 * @param status
	 * @param contentType
	 * @param headers
	 */
	public void sendContent(String content, HttpError status, String contentType, Headers headers) {
		HttpServerResponse response = response(status)
		.putHeader("Content-Type", contentType)
		.putHeader("Content-Length", String.valueOf(content.length()));
		for(Map.Entry<String, String> header : headers.entrySet()) {
			response.putHeader(header.getKey(), header.getValue());
		}
		response.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content
	 * @param status
	 * @param contentType
	 */
	public void sendContent(String content, HttpError status, String contentType) {
		response(status)
		.putHeader("Content-Type", contentType)
		.putHeader("Content-Length", String.valueOf(content.length()))
		.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content
	 * @param contentType
	 */
	public void sendContent(String content, String contentType) {
		response(new OK())
		.putHeader("Content-Type", contentType)
		.putHeader("Content-Length", String.valueOf(content.length()))
		.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content
	 * @param status
	 */
	public void sendContent(String content, HttpError status) {
		response(status)
		.putHeader("Content-Type", "text/plain")
		.putHeader("Content-Length", String.valueOf(content.length()))
		.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content
	 */
	public void sendContent(String content) {
		response(new OK())
		.putHeader("Content-Type", "text/plain")
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
		if(Objects.nonNull(err.getHeaders())) 
			sendJSON(new JsonObject().put("status", false).put("message", err.getMessage()), err, err.getHeaders());
		else
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
