package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of a non-standard authentication scheme where a single opaque token is provided
 * without an authencation scheme name
 * @author odeda
 */
public class SimpleAuthrizationToken extends AuthorizationToken {

	public SimpleAuthrizationToken() {}
	
	public SimpleAuthrizationToken(String token) {
		update("Simple", token);
	}

}
