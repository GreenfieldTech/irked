package tech.greenfield.vertx.irked.exceptions;

import tech.greenfield.vertx.irked.Request;
import tech.greenfield.vertx.irked.status.BadRequest;

/**
 * Exception thrown from {@link Request#getBodyAs(Class)} when the request contains
 * no body at all.
 */
public class MissingBodyException extends BadRequest {

	private static final long serialVersionUID = 4458613845508649207L;
	
	/**
	 * Create a new exception
	 */
	public MissingBodyException() {
		super("Required request body is missing");
	}

}
