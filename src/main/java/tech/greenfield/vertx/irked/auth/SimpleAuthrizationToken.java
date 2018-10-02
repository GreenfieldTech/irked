package tech.greenfield.vertx.irked.auth;

public class SimpleAuthrizationToken extends AuthorizationToken {

	public SimpleAuthrizationToken() {}
	
	public SimpleAuthrizationToken(String token) {
		update("Simple", token);
	}

}
