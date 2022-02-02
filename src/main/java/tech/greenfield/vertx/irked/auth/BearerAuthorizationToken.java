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
	
	public BearerAuthorizationToken() {}
	
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
