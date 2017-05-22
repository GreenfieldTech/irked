package tech.greenfield.vertx.irked;

import java.util.Objects;

public class HttpError extends Exception {

	private static final long serialVersionUID = -7084405660609573926L;
	
	private int statusCode;
	private String statusText;
	
	public HttpError(int statusCode, String statusText) {
		super();
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public HttpError(int statusCode, String statusText, String message) {
		super(message);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
	public HttpError(int statusCode, String statusText, Throwable throwable) {
		super(throwable);
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
	
	/**
	 * Helper method to make it easier to throw HTTP statuses out of lambdas.
	 * Outside the lambda you should catch a {@link RuntimeException} and use
	 * {@link #unwrap()} to get the original exception
	 * @return {@link RuntimeException} wrapping this status instance
	 */
	public RuntimeException uncheckedWrap() {
		return new RuntimeException(this);
	}
	
	/**
	 * Unwrap {@link RuntimeException} wrappers around a logical exception
	 * (hopefully an instance of HttpError)
	 * @param t Throweable to unwrap
	 * @return the first non RuntimeException found
	 */
	public static Throwable unwrap(Throwable t) {
		while (t instanceof RuntimeException && Objects.nonNull(t.getCause()))
			t = t.getCause();
		return t;
	}

}
