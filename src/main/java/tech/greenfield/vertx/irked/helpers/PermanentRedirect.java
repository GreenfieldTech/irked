package tech.greenfield.vertx.irked.helpers;

import tech.greenfield.vertx.irked.status.MovedPermanently;

@SuppressWarnings("serial")
public class PermanentRedirect extends MovedPermanently {
	
	public PermanentRedirect(String location) {
		addHeader("Location", location);
	}

	public PermanentRedirect(String message, String location) {
		super(message);
		addHeader("Location", location);
	}

}
