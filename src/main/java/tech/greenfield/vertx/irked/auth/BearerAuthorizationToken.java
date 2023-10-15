package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of the standard RFC 6750 Bearer authentication scheme token
 * 
 * No additional parsing is done on the bearer token and the user is expected to implement
 * their own verification on the token value that can be retrieved from the {@link AuthorizationToken#getToken()} method.
 * 
 * @author odeda
 */
public class BearerAuthorizationToken extends AuthorizationToken {
	
	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public BearerAuthorizationToken() {}
	
	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
	public BearerAuthorizationToken(String token) {
		update("Bearer", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "Bearer".equalsIgnoreCase(type);
	}
	

}
