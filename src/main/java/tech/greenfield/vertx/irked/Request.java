package tech.greenfield.vertx.irked;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextDecorator;
import tech.greenfield.vertx.irked.Controller.WebHandler;
import tech.greenfield.vertx.irked.auth.AuthorizationToken;
import tech.greenfield.vertx.irked.exceptions.MissingBodyException;
import tech.greenfield.vertx.irked.status.BadRequest;
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
	public void next() {
		this.outerContext.next();
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
	 * This method will call {@link Request#fail(Throwable)} if a failure occurred,
	 * after unwrapping {@link RuntimeException}s as needed. It will also pass on
	 * the success value (or null if there was a failure) for the next async
	 * element. Subsequent code can check whether a failure was propagated
	 * by calling {@link #failed()}
	 * @param successValue successful completion value to return in case no failure occurred
	 * @param throwable A {@link Throwable} error to fail on
	 * @return null
	 */
	public <V> V handlePossibleFailure(V successValue, Throwable throwable) {
		if (Objects.nonNull(throwable))
			fail(HttpError.unwrap(throwable));
		return successValue;
	}
	
	/**
	 * Helper to easily configure standard failure handlers
	 * @return a WebHandler that sends Irked status exceptions as HTTP responses
	 */
	public static WebHandler failureHandler() {
		return r -> {
			r.sendError(HttpError.toHttpError(r));
		};
	}
	
	public <T> T getBodyAs(Class<T> type) {
		String contentType = this.request().getHeader("Content-Type");
		if (Objects.isNull(contentType)) contentType = "application/json"; // we love JSON
		String[] ctParts = contentType.split(";\\s*");
		switch (ctParts[0]) {
		case "application/x-www-form-urlencoded":
			JsonObject out = new JsonObject();
			request().formAttributes().forEach(e -> {
				Object old = out.getValue(e.getKey());
				if (Objects.isNull(old)) {
					out.put(e.getKey(), e.getValue());
					return;
				}
				if (old instanceof JsonArray) {
					((JsonArray)old).add(e.getValue());
					return;
				}
				out.put(e.getKey(), new JsonArray().add(old).add(e.getValue()));
			});
			return out.mapTo(type);
		case "application/json":
		default:
			try {
				JsonObject body = getBodyAsJson();
				if (body == null)
					throw new MissingBodyException().unchecked();
				return body.mapTo(type);
			} catch (DecodeException e) {
				throw new BadRequest("Unrecognized content-type " + ctParts[0] + 
						" and content does not decode as JSON: " + e.getMessage()).unchecked();
			}
		}
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
	 * Helper method to terminate a request processing with a success (200 OK) response
	 * containing a JSON object mapped from the specified POJO
	 * @param data POJO containing the data to map to a JSON encoded object
	 */
	public void sendObject(Object data) {
		sendJSON(JsonObject.mapFrom(data));
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
	 * @param status An HttpError object representing the HTTP status to be sent
	 */
	public void sendJSON(JsonObject json, HttpError status) {
		sendContent(json.encode(), status, "application/json");
	}
	
	/**
	 * Helper method to terminate a request processing with a custom response
	 * containing a JSON object mapped from the specified POJO and the specified status line.
	 * @param data POJO containing the data to map to a JSON encoded object
	 * @param status An HttpError object representing the HTTP status to be sent
	 */
	public void sendObject(Object data, HttpError status) {
		sendJSON(JsonObject.mapFrom(data), status);
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
	 * containing some text and the specified status line.
	 * @param content Text content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @param contentType The MIME Content-Type to be set for the response
	 */
	public void sendContent(String content, HttpError status, String contentType) {
		sendContent(Buffer.factory.buffer(content), status, contentType);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some data and the specified status line.
	 * @param content Binary content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @param contentType The MIME Content-Type to be set for the response
	 */
	public void sendContent(Buffer content, HttpError status, String contentType) {
		response(status)
				.putHeader("Content-Type", contentType)
				.putHeader("Content-Length", String.valueOf(content.length()))
				.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 * @param contentType The MIME Content-Type to be set for the response
	 */
	public void sendContent(String content, String contentType) {
		sendContent(content, new OK(), contentType);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 */
	public void sendContent(String content, HttpError status) {
		sendContent(content, status, "text/plain");
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 */
	public void sendContent(String content) {
		sendContent(content, new OK(), "text/plain");
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP error (non-200 OK) response.
	 * The resulting HTTP response will have the correct status line and an application/json content
	 * with a JSON encoded object containing the fields "status" set to "false" and "message" set
	 * to the {@link HttpError}'s message.
	 * @param status An HttpError object representing the HTTP status to be sent
	 */
	public void sendError(HttpError status) {
		sendJSON(new JsonObject().put("status", status.getStatusCode() / 100 == 2).put("message", status.getMessage()), status);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param object {@link JsonObject} of data to send
	 */
	public void send(JsonObject object) {
		sendJSON(object);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param list {@link JsonArray} of a list of data to send
	 */
	public void send(JsonArray list) {
		sendJSON(list);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a text/plain response
	 * @param content text to send
	 */
	public void send(String content) {
		sendContent(content);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a application/octet-stream response
	 * @param buffer binary data to send
	 */
	public void send(Buffer buffer) {
		sendContent(buffer, new OK(), "application/octet-stream");
	}
	
	/**
	 * Helper method to terminate request processing with a non-OK HTTP response with default text
	 * @param status {@link HttpError} to send
	 */
	public void send(HttpError status) {
		sendError(status);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and an application/json
	 * response containing a list of {@link io.vertx.core.json.Json}-encoded objects
	 * @param <G> type of objects in the list
	 * @param list List to convert to a JSON array for sending
	 */
	public <G> void sendList(List<G> list) {
		sendStream(list.stream());
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and an application/json
	 * response containing a stream of {@link io.vertx.core.json.Json}-encoded objects.
	 * Please note that the response will be buffered in memory using a {@link io.vertx.core.json.JsonArray}
	 * based collector.
	 * @param <G> type of objects in the stream
	 * @param stream Stream to convert to a JSON array for sending
	 */
	public <G> void sendStream(Stream<G> stream) {
		sendJSON(stream.map(this::encodeToJsonType).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param object custom object to process through Jackson's {@link ObjectMapper} to generate JSON content
	 */
	@SuppressWarnings("unchecked")
	public void send(Object object) {
		if (object instanceof List)
			sendList((List<Object>)object);
		else if (object instanceof Stream)
			sendStream((Stream<Object>)object);
		else
			sendObject(object);
	}

	/**
	 * Helper method to generate response with the specified HTTP status
	 * @param status HTTP status code and text to set on the response
	 * @return HTTP response created using {@link RoutingContext#response()}
	 */
	public HttpServerResponse response(HttpError status) {
		HttpServerResponse res = response();
		for (Entry<String, String> h : status.getHeaders())
			res.putHeader(h.getKey(), h.getValue());
		return res.setStatusCode(status.getStatusCode()).setStatusMessage(status.getStatusText());
	}
	
	/**
	 * Check if the client requested a connection upgrade, regardless which type
	 * of upgrade is required.
	 * @return {@literal true} if the request includes a 'Connection: upgrade' header.
	 */
	public boolean needUpgrade() {
		return needUpgrade(null);
	}
	
	/**
	 * check if the client requested a specific connection upgrade.
	 * @param type What upgrade type to test against, case insensitive
	 * @return {@literal true} if the request includes a 'Connection: upgrade' header and an 'Upgrade' header with the specified type.
	 */
	public boolean needUpgrade(String type) {
		HttpServerRequest req = request();
		return req.getHeader("Connection").equalsIgnoreCase("upgrade") && (Objects.isNull(type) || req.getHeader("Upgrade").equalsIgnoreCase(type));
	}
	
	/**
	 * Helper for authorization header parsing
	 * @return A parsed {@link AuthorizationToken}
	 */
	public AuthorizationToken getAuthorization() {
		return AuthorizationToken.parse(request().getHeader("Authorization"));
	}

	/**
	 * Helper method to encode arbitrary types to a type that Vert.x 
	 * @{link io.vertx.json.JsonObject} and @{link io.vertx.json.JsonArray}
	 * will accept.
	 * 
	 * This implementation recognizes some types as Vert.x JSON "safe" (i.e. can
	 * be used with <code>JsonArray::add</code> but not all the types that would be
	 * accepted. Ideally we would use Json.checkAndCopy() but it is not visible.
	 * @param value object to recode to a valid JSON type
	 * @return a type that will be accepted by JsonArray.add();
	 */
	private Object encodeToJsonType(Object value) {
		if (value instanceof Boolean ||
				value instanceof Number ||
				value instanceof String ||
				value instanceof JsonArray ||
				value instanceof List ||
				value instanceof JsonObject ||
				value instanceof Map)
			return value;
		return JsonObject.mapFrom(value);
	}

}
