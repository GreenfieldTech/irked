package tech.greenfield.vertx.irked.auth;

public class NullAuthorizationToken extends AuthorizationToken {

	public NullAuthorizationToken() {
		update(null, null);
	}

}
