package tech.greenfield.vertx.irked;

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

}
