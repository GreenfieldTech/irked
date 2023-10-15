package tech.greenfield.vertx.irked.helpers;

import tech.greenfield.vertx.irked.status.MovedPermanently;

/**
 * A 301 Moved Permanently response that encodes the destination location
 */
@SuppressWarnings("serial")
public class PermanentRedirect extends MovedPermanently {
	
	/**
	 * Create a new 301 Moved Permanently redirection to the specified location
	 * @param location URI which the client should load next
	 */
	public PermanentRedirect(String location) {
		addHeader("Location", location);
	}

	/**
	 * Create a new 301 Moved Permanently redirection to the specified location, with the specified message
	 * @param message message to encode in the body of the response
	 * @param location URI which the client should load next
	 */
	public PermanentRedirect(String message, String location) {
		super(message);
		addHeader("Location", location);
	}

}
