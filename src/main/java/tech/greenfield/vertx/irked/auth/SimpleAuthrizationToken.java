package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of a non-standard authentication scheme where a single opaque token is provided
 * without an authentication scheme name
 * @author odeda
 */
public class SimpleAuthrizationToken extends AuthorizationToken {

	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public SimpleAuthrizationToken() {}
	
	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
	public SimpleAuthrizationToken(String token) {
		update("Simple", token);
	}

}
