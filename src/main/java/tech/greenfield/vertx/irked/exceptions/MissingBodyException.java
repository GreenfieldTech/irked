package tech.greenfield.vertx.irked.exceptions;

import tech.greenfield.vertx.irked.status.BadRequest;

public class MissingBodyException extends BadRequest {

	private static final long serialVersionUID = 4458613845508649207L;
	
	public MissingBodyException() {
		super("Required request body is missing");
	}

}
