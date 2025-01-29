package tech.greenfield.vertx.irked;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextDecorator;
import io.vertx.ext.web.impl.RoutingContextInternal;
import tech.greenfield.vertx.irked.Controller.WebHandler;
import tech.greenfield.vertx.irked.auth.AuthorizationToken;
import tech.greenfield.vertx.irked.exceptions.MissingBodyException;
import tech.greenfield.vertx.irked.helpers.JsonDecodingExceptionFormatter;
import tech.greenfield.vertx.irked.status.BadRequest;
import tech.greenfield.vertx.irked.status.HttpStatuses;

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

	private static RoutingContextInternal downCastOrFailWithExplanation(RoutingContext outerContext) {
		if (outerContext instanceof RoutingContextInternal)
			return (RoutingContextInternal) outerContext;
		/*
		 * This is an issue because of https://github.com/vert-x3/vertx-web/commit/65972a2e43a853ae6a226a25cc24351d685e0a44
		 * Under the guise of "Feature." [sic], required functionality was added to the (supposedly internal) RoutingContextInternal
		 * extension interface and then requiring it for the RoutingContextDecorator c'tor, but without modifying all the other APIs
		 * that still use RoutingContext. If you think this code here is bad, than check out RouterImpl that does an *unchecked* cast down:
		 * https://github.com/vert-x3/vertx-web/blob/e9430acf6edd029ddc80bcb00f87a56e10171312/vertx-web/src/main/java/io/vertx/ext/web/impl/RouterImpl.java#L248
		 * 
		 * At the time of this writing all vertx-web implementations of RoutingContext actually implement RoutingContextInternal, and all are
		 * created by RouterImpl - which we use explicitly through io.vertx.ext.web.Router.router() (in tech.greenfield.vertx.irked.Router), but
		 * this here now relies on vertx-web developers always paying attention instead on compiler type checking...
		 */
		throw new RuntimeException("Unexpected parent context that does not implement RoutingContextInternal! This is a bug in vertx-web 4.2.2");
	}
	
	public static final String USE_JSON_PRETTY_ENCODER = "irked.json-pretty-encoder";
	
	private RoutingContext outerContext;
	private boolean usePrettyEncoder = false;

	/**
	 * Create a new request wrapper as a {@link RoutingContextDecorator} around the specified parent routing context
	 * (what vertx-web calls "inner context").
	 * 
	 * This is an internal constructor to be used by {@link Router} - use at your own risk.
	 * @param outerContext parent routing context to wrap
	 */
	public Request(RoutingContext outerContext) {
		super(outerContext.currentRoute(), downCastOrFailWithExplanation(outerContext));
		this.outerContext = outerContext;
		usePrettyEncoder = Objects.requireNonNullElse(this.outerContext.get(USE_JSON_PRETTY_ENCODER), usePrettyEncoder);
	}
	
	/**
	 * Set the built-in JSON encoding behavior of {@linkplain Request} to use either the {@link Json#encodePrettily(Object)}
	 * behavior instead of the standard compact encoding, or not.
	 * 
	 * To enable this behavior to an entire controller hierarchy, use it in the controller's
	 * {@link Controller#getRequestContext(Request)} override.
	 * @param usePrettyEncoder set to {@code true} to enable the pretty encoder
	 * @return itself for fluent calls
	 */
	public Request setJsonEncoding(boolean usePrettyEncoder) {
		put(USE_JSON_PRETTY_ENCODER, this.usePrettyEncoder = usePrettyEncoder);
		return this;
	}
	
	@Override
	public void next() {
		this.outerContext.next();
	}

	/**
	 * Helper to attach {@link #next()} to Vert.x {@link Future#onSuccess(io.vertx.core.Handler)} handler.
	 * The input value is ignored and it is just defined so that you can do {@code asyncAPI().onSuccess(r::next)}
	 * 
	 * You often will want to store the result on the routing context for use in the next handler, so a more
	 * complete example might look like this:
	 * 
	 * <pre>{@code
	 * asyncAPI()
	 * .onSuccess(data -> r.put("data", data))
	 * .onSuccess(r::next)
	 * .onFailure(r::handleFailure);
	 * }</pre>
	 * @param <T> Type of value provided by handler caller, that will be ignored
	 * @param value result provided by handler caller, that will be ignored
	 */
	public <T> void next(T value) {
		this.outerContext.next();
	}

	@Override
	public void fail(int statusCode) { 
		// we're overriding the fail handlers, which for some reason the decorator 
		// feels should be moved to another thread. Instead, use the outer implementation
		// and let it do what's right
		this.outerContext.fail(statusCode);
	}
	
	/**
	 * Fail helper that wraps Irked HTTP errors in Vert.x-web (final?!) HttpException class
	 * that is better handled by the RoutingContextImpl
	 * @param httpError error to fail with
	 */
	public void fail(HttpError httpError) {
		fail(httpError.getStatusCode(), httpError);
	}
	
	@Override
	public void fail(Throwable throwable) {
		// we're overriding the fail handlers, which for some reason the decorator 
		// feels should be moved to another thread. Instead, use the outer implementation
		// and let it do what's right
		this.outerContext.fail(Objects.requireNonNull(throwable));
	}
	
	/**
	 * Helper failure handler for Promise/CompletableFuture users.
	 * Use at the end of an async chain to succinctly propagate exceptions, such as:
	 * <code>.onFailure(r::handleFailure)</code> or <code>.exceptionally(req::handleFailure)</code>.
	 * This method will call {@link #fail(Throwable)} after unwrapping
	 * {@link RuntimeException}s as needed.
	 * @param throwable A {@link Throwable} error to fail on
	 * @return null
	 */
	public Void handleFailure(Throwable throwable) {
		var failure = HttpError.unwrap(throwable);
		if (failure instanceof HttpError)
			fail((HttpError)failure);
		else
			fail(failure);
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
	 * @param <V> the type of previous completion value that will be returned as the completion value for completion stages running this method 
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
	 * Helper to easily configure standard failure handlers.
	 * This handler will convert failed requests (contexts with a failure set) to HTTP error responses, and will issue
	 * {@link RoutingContext#next()} for all other context, including context for which the response has already been
	 * sent.
	 * @return a WebHandler that sends Irked status exceptions as HTTP responses
	 */
	public static WebHandler failureHandler() {
		return r -> {
			if (r.response().ended() || !r.failed())
				r.next();
			else
				r.sendError(HttpError.toHttpError(r));
		};
	}
	
	/**
	 * Convert request body to an instance of the specified POJO
	 * 
	 * Currently the followed request body content types are supported:
	 *  * <code>application/json</code> - the body is read using {@link RoutingContext#getBodyAsJson()} then
	 *    mapped to the bean type using {@link JsonObject#mapTo(Class)}
	 *  * <code>application/x-www-form-urlencoded</code> - the body is read using {@link HttpServerRequest#formAttributes()}
	 *   into a {@link JsonObject} as keys with string values, then mapped to the bean type using {@link JsonObject#mapTo(Class)}.
	 *   If the same key is present multiple times, the values will be stored into the JsonObject as a {@link JsonArray} with string values.
	 * 
	 * If no content-type header is specified in the request, <code>application/json</code> is assumed.
	 * 
	 * If no body is present, this method will throw an unchecked {@link MissingBodyException} - i.e. a "Bad Request"
	 * HTTP error with the text "Required request body is missing".
	 * 
	 * @apiNote this API is very similar to the Vert.x 4.3 API {@link RequestBody#asPojo(Class)}. The notable difference
	 *   is that this implementation checks the request Content-Type and if its a form POST, it will read the form post fields
	 *   to mimic a JSON object. This may or may not be a desired behavior.
	 * 
	 * @param <T> Result type into which the body will be decoded
	 * @param type The class from which an instance should be created to hold the body content
	 * @return An object of the specified type that was initialized using the Vert.x JSON decoder API
	 * @throws RuntimeException in case of a JSON decoding problem. The thrown exception wraps a
	 *   {@linkplain BadRequest} exception with a REST API friendly error message (so it can be handled
	 *   automatically by {@link #handleFailure(Throwable)}) and that wraps the original {@link DecodeException}
	 *   if the caller wants to extract it and parse it themselves
	 */
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
				T body = body().asPojo(type);
				if (body == null)
					throw new MissingBodyException().unchecked();
				return body;
			} catch (DecodeException e) {
				throw new BadRequest(JsonDecodingExceptionFormatter.formatFriendlyErrorMessage(e), e).unchecked();
			}
		}
	}
	
	/**
	 * Helper method to terminate request processing with a success (200 OK) response 
	 * containing a JSON body.
	 * @param json {@link JsonObject} containing the output to encode
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendJSON(JsonObject json) {
		return sendJSON(json, statusFromResponseCode());
	}
	
	/**
	 * Helper method to terminate a request processing with a success (200 OK) response
	 * containing a JSON object mapped from the specified POJO
	 * @param data POJO containing the data to map to a JSON encoded object
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendObject(Object data) {
		try {
			return sendJSON(JsonObject.mapFrom(data));
		} catch (Throwable t) { // JsonObject.mapFrom run user's code that may throw by mistake
			return Future.failedFuture(t);
		}
	}
	
	/**
	 * Helper method to terminate request processing with a success (200 OK) response 
	 * containing a JSON body.
	 * @param json {@link JsonArray} containing the output to encode
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendJSON(JsonArray json) {
		return sendJSON(json, statusFromResponseCode());
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonObject} containing the output to encode
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendJSON(JsonObject json, HttpError status) {
		return sendContent(Json.CODEC.toString(json, usePrettyEncoder), status, "application/json");
	}
	
	/**
	 * Helper method to terminate a request processing with a custom response
	 * containing a JSON object mapped from the specified POJO and the specified status line.
	 * @param data POJO containing the data to map to a JSON encoded object
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendObject(Object data, HttpError status) {
		try {
			return sendJSON(JsonObject.mapFrom(data), status);
		} catch (Throwable t) { // JsonObject.mapFrom run user's code that may throw by mistake
			return Future.failedFuture(t);
		}
	}
	
	/**
	 * Helper method to terminate request processing with a custom response 
	 * containing a JSON body and the specified status line.
	 * @param json {@link JsonArray} containing the output to encode
	 * @param status HTTP status to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendJSON(JsonArray json, HttpError status) {
		return sendContent(Json.CODEC.toString(json, usePrettyEncoder), status, "application/json");
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specified status line.
	 * @param content Text content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @param contentType The MIME Content-Type to be set for the response
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendContent(String content, HttpError status, String contentType) {
		return sendContent(Buffer.buffer(content), status, contentType);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some data and the specified status line.
	 * @param content Binary content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @param contentType The MIME Content-Type to be set for the response
	 * @return a promise that will resolve when the body was sent successfully
	 */
	public Future<Void> sendContent(Buffer content, HttpError status, String contentType) {
		var res = response(status)
				.putHeader("Content-Type", contentType)
				.putHeader("Content-Length", String.valueOf(content.length()));
		if (isHead())
			return res.end();
		return res.end(content);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 * @param contentType The MIME Content-Type to be set for the response
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendContent(String content, String contentType) {
		return sendContent(content, statusFromResponseCode(), contentType);
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendContent(String content, HttpError status) {
		return sendContent(content, status, "text/plain");
	}
	
	/**
	 * Helper method to terminate request processing with a custom response
	 * containing some text and the specifeid status line.
	 * @param content Text content to send in the response
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendContent(String content) {
		return sendContent(content, statusFromResponseCode(), "text/plain");
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP error (non-200 OK) response.
	 * The resulting HTTP response will have the correct status line and an application/json content
	 * with a JSON encoded object containing the fields "status" set to "false" and "message" set
	 * to the {@link HttpError}'s message.
	 * @param status An HttpError object representing the HTTP status to be sent
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> sendError(HttpError status) {
		return sendJSON(new JsonObject().put("status", status.getStatusCode() / 100 == 2).put("message", status.getMessage()), status);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param object {@link JsonObject} of data to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> send(JsonObject object) {
		return sendJSON(object);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param list {@link JsonArray} of a list of data to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> send(JsonArray list) {
		return sendJSON(list);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a text/plain response
	 * @param content text to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> send(String content) {
		return sendContent(content);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a application/octet-stream response
	 * @param buffer binary data to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> send(Buffer buffer) {
		return sendContent(buffer, statusFromResponseCode(), "application/octet-stream");
	}
	
	/**
	 * Helper method to terminate request processing with a non-OK HTTP response with default text
	 * @param status {@link HttpError} to send
	 * @return a promise that will complete when the body was sent successfully
	 */
	public Future<Void> send(HttpError status) {
		return sendError(status);
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and an application/json
	 * response containing a list of {@link io.vertx.core.json.Json}-encoded objects
	 * @param <G> type of objects in the list
	 * @param list List to convert to a JSON array for sending
	 * @return a promise that will complete when the body was sent successfully
	 */
	public <G> Future<Void> sendList(List<G> list) {
		return sendStream(list.stream());
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and an application/json
	 * response containing a stream of {@link io.vertx.core.json.Json}-encoded objects.
	 * Please note that the response will be buffered in memory using a {@link io.vertx.core.json.JsonArray}
	 * based collector.
	 * @param <G> type of objects in the stream
	 * @param stream Stream to convert to a JSON array for sending
	 * @return a promise that will complete when the body was sent successfully
	 */
	public <G> Future<Void> sendStream(Stream<G> stream) {
		try {
			return sendJSON(stream.map(this::encodeToJsonType).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
		} catch (Throwable t) { // JsonObject.mapFrom run user's code that may throw by mistake
			return Future.failedFuture(t);
		}
	}
	
	/**
	 * Helper method to terminate request processing with an HTTP OK and a JSON response
	 * @param object any object that make sense to convert to JSON for sending. Converts lists and streams to arrays
	 * using {@linkplain #sendList(List)} and {@linkplain #sendStream(Stream)}; exceptions and {@link HttpError}s to
	 * HTTP status descriptions using {@linkplain #sendError(HttpError)} and everything else maps to JSON using
	 * {@linkplain #sendObject}.
	 * @return a promise that will complete when the body was sent successfully
	 */
	@SuppressWarnings("unchecked")
	public Future<Void> send(Object object) {
		if (object instanceof List)
			return sendList((List<Object>)object);
		else if (object instanceof Stream)
			return sendStream((Stream<Object>)object);
		else if (object instanceof Throwable)
			return sendError(HttpError.toHttpError((Throwable)object));
		else if (object instanceof JsonObject)
			return sendJSON((JsonObject)object);
		else if (object instanceof JsonArray)
			return sendJSON((JsonArray)object);
		else if (object instanceof String)
			return sendContent((String)object);
		else
			return sendObject(object);
	}
	
	/**
	 * Helper method to terminate request processing with either an HTTP OK (or whatever response status is currently set)
	 * using {@link #send(Object)} if the result is a successful result, or handle the failure using {@link #handleFailure(Throwable)}
	 * if the result is of a failure.
	 * 
	 * This can be used as a shorthand for the common pattern of <code>.compose(req::send).onFailure(req::handleFailure)</code>,
	 * instead the request processing chain can be terminated using either of these patterns:
	 * <ul>
	 * <li><code>… .onComplete(req::sendOrFail);</code></li>
	 * <li><code>… .andThen(req::sendOrFail);</code></li>
	 * <li><code>… .transform(req::sendOrFail);</code></li>
	 * </ul>
	 * <p>
	 * In the first two cases, the {@linkplain #sendOrFail(AsyncResult)} method has no side effects for the composition. In the last
	 * case, the result of the transform is a {@code Future<Void>} that will fail if either the previous step has failed
	 * - with the original cause - or if the {@linkplain #send(Object)} has failed - with the send failure cause - or will
	 * succeed if the {@linkplain #send(Object)} has succeeded. But you will probably not care about the difference if you
	 * use this as a terminal operation.
	 * </p>
	 * @param <T> Type of value in a successful result
	 * @param result a possible success or failure result
	 * @return a promise that will fail if the result has failed or if sending a successful result has failed, or will
	 *   succeed if sending the successful result has succeeded.
	 */
	public <T> Future<Void> sendOrFail(AsyncResult<T> result) {
		if (result.failed()) {
			handleFailure(result.cause());
			return Future.failedFuture(result.cause());
		}
		return send(result.result())
				.onFailure(this::handleFailure);
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

	/**
	 * Check if this request is a HEAD request, in which case {@link #sendContent(Buffer, HttpError, String)}
	 * will not send any content (but will send all headers including content-type and content-length).
	 * @return whether the request is a HEAD request
	 */
	public boolean isHead() {
		return request().method() == HttpMethod.HEAD;
	}

	private static final String SPECIFIC_FAILURE_FLD = String.format("%1$s.specific-failure", Request.class);

	/**
	 * When looking at a failed routing context, try to find a specific type of exception from the
	 * failure and cause chain. This helper makes the {@code @OnFail(exception)} annotation useful
	 * by allowing the user to quickly extract the specific exception instance they are looking for.
	 * @param <G> Type of exception to extract
	 * @param failureType the class of the exception to extract
	 * @return The top-most exception of the specified type from the failure and cause chain, or {@code null}
	 *   if this request has not failed yet or the specified type isn't found in the failure chain
	 */
	public <G extends Throwable> G findFailure(Class<G> failureType) {
		if (!failed())
			return null;
		Throwable specificFailure = get(SPECIFIC_FAILURE_FLD);
		if (failureType.isInstance(specificFailure)) {
			@SuppressWarnings("unchecked")
			G actual = (G) specificFailure;
			return actual;
		}
		// else...
		for (Throwable f = failure(); f != null; f = f.getCause())
			if (failureType.isInstance(f)) {
				@SuppressWarnings("unchecked")
				G actual = (G) f;
				return actual;
			}
		// else...
		return null;
	}
	
	/**
	 * Store a specific exception type, supposedly that was found by am {@code @OnFail(exception)} processor
	 * so it can be efficiently retrieved by {@link #findFailure(Class)}.
	 * @param failure specific failure found by the {@code @OnFail(exception)} processor
	 * @return itself, for fleunt access
	 */
	public Request setSpecificFailure(Throwable failure) {
		if (failed())
			put(SPECIFIC_FAILURE_FLD, failure);
		return this;
	}

	public HttpError statusFromResponseCode() {
		return statusFromResponseCode(response().getStatusCode(), response().getStatusMessage());
	}

	public HttpError statusFromResponseCode(int statusCode) {
		return statusFromResponseCode(statusCode, HttpResponseStatus.valueOf(statusCode).reasonPhrase());
	}
	
	public HttpError statusFromResponseCode(int statusCode, String statusMessage) {
		try {
			return HttpStatuses.create(statusCode).setStatusText(statusMessage);
		} catch (InstantiationException e) {
			return new HttpError(statusCode, statusMessage);
		}
	}
}
