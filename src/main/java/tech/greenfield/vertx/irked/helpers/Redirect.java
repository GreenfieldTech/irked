package tech.greenfield.vertx.irked.helpers;

import tech.greenfield.vertx.irked.status.Found;

@SuppressWarnings("serial")
public class Redirect extends Found {

	public Redirect(String location) {
		addHeader("Location", location);
	}

	public Redirect(String message, String location) {
		super(message);
		addHeader("Location", location);
	}

}
