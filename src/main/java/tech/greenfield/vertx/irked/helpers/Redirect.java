package tech.greenfield.vertx.irked.helpers;

import tech.greenfield.vertx.irked.status.Found;

/**
 * A 302 Found response that encodes the destination location
 */
@SuppressWarnings("serial")
public class Redirect extends Found {

	/**
	 * Create an HTTP 302 Found response with the specified value for the <code>Location</code> header
	 * @param location URL to set for the <code>Location</code> header
	 */
	public Redirect(String location) {
		addHeader("Location", location);
	}

	/**
	 * Create an HTTP 302 Found response with the specified value for the <code>Location</code> header
	 * @param message exception message to set
	 * @param location URL to set for the <code>Location</code> header
	 */
	public Redirect(String message, String location) {
		super(message);
		addHeader("Location", location);
	}

}
