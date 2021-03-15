package tech.greenfield.vertx.irked;

import java.util.*;

import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.status.HttpStatuses;
import tech.greenfield.vertx.irked.status.InternalServerError;
import tech.greenfield.vertx.irked.status.OK;

public class HttpError extends Exception {

	public class UncheckedHttpError extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private UncheckedHttpError() {
			super(HttpError.this);
		}
	}

	private static final long serialVersionUID = -7084405660609573926L;
	
	private int statusCode;
	private String statusText;
	private HeadersMultiMap headers = HeadersMultiMap.httpHeaders();
	
	public HttpError(int statusCode, String statusText) {
		super(statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public HttpError(int statusCode, String statusText, String message) {
		super(message);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public HttpError(int statusCode, String statusText, Throwable throwable) {
		super(statusText, throwable);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public HttpError(int statusCode, String statusText, String message, Throwable throwable) {
		super(message, throwable);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public String getStatusText() {
		return statusText;
	}
	
	public HttpError addHeader(String header, String value) {
		this.headers.add(header, value);
		return this;
	}
	
	public MultiMap getHeaders() {
		return headers;
	}
	
	/**
	 * Alias to {@link #unchecked()}
	 * @return unchecked {@link RuntimeException} wrapping this status instance
	 */
	public RuntimeException uncheckedWrap() {
		return new UncheckedHttpError();
	}
	
	/**
	 * Helper method to make it easier to throw HTTP statuses out of lambdas.
	 * Outside the lambda you should catch a {@link RuntimeException} and use
	 * {@link HttpError#unwrap(Throwable)} to get the original exception
	 * @return unchecked {@link RuntimeException} wrapping this status instance
	 */
	public RuntimeException unchecked() {
		return new UncheckedHttpError();
	}
	
	/**
	 * Unwrap {@link RuntimeException} wrappers around a logical exception
	 * (hopefully an instance of HttpError)
	 * @param t Throweable to unwrap
	 * @return the first non RuntimeException found
	 */
	public static Throwable unwrap(Throwable t) {
		Throwable orig = t;
		while (!(t instanceof HttpError)) {
			if (Objects.isNull(t.getCause()))
				return orig; // can't find HTTP Error
			t = t.getCause();
		}
		return t; // must be an HTTP Error
	}
	
	/**
	 * Helper method for OnFail handlers to locate a wrapped HttpError or
	 * create an InternalServerError from an unexpected exception (whether it
	 * is wrapped or not)
	 * @param t Throwable to be analyzed
	 * @return the wrapped HttpError instance or a new {@link InternalServerError} wrapping
	 *   the real exception
	 */
	public static HttpError toHttpError(Throwable t) {
		t = unwrap(t);
		if (t instanceof HttpError)
			return (HttpError)t;
		return new InternalServerError(t);
	}
	
	/**
	 * Helper method for OnFail handlers to create an appropriate HTTP error class
	 * for a failed {@link RoutingContext}, by reading the failed status code or
	 * failure exception. 
	 * 
	 * If {@link RoutingContext#failure()} returns a (possibly wrapped) 
	 * {@link HttpError} then that what will be returned, otherwise either an
	 * {@link InternalServerError} will be returned (for an exception failure) or
	 * an appropriate default HTTP status instance according to the failed status code.
	 * 
	 * Note that if the {@link RoutingContext#failed()} {@code == false}, then an {@link OK}
	 * HTTP status class instance will be returned.
	 * @param ctx failed {@code RoutingContext} to investigate
	 * @return an {@code HttpError} instance representing the status of the {@code RoutingContext}
	 */
	public static HttpError toHttpError(RoutingContext ctx) {
		if (!ctx.failed())
			return new OK();
		if (Objects.nonNull(ctx.failure()))
			return toHttpError(ctx.failure());
		if (!HttpStatuses.HTTP_STATUS_CODES.containsKey(ctx.statusCode()))
			return new InternalServerError("Unknown HTTP status code " + ctx.statusCode());
		try {
			return HttpStatuses.create(ctx.statusCode());
		} catch (InstantiationException e) {
			// should never happen, assuming we know all valid HTTP status codes
			return new InternalServerError("Failed to translate failed context to HTTP error");
		}
	}
}
