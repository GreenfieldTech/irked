package tech.greenfield.vertx.irked.exceptions;

/**
 * Exception thrown during Irked routing configuration validation, if there is an error in the configuration. 
 */
public class InvalidRouteConfiguration extends Exception {

	private static final long serialVersionUID = 4458613845508649189L;
	
	/**
	 * Create a new routing configuration validation error
	 * @param message error details
	 */
	public InvalidRouteConfiguration(String message) {
		super(message);
	}

}
