package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of the standard RFC 4559 Negotiate authentication scheme token
 * 
 * This implementation does not decode the GSS API data and it is available as plain text
 * from the {@link AuthorizationToken#getToken()} method.
 * 
 * @author odeda
 */
public class NegotiateAuthorizationToken extends AuthorizationToken {

	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public NegotiateAuthorizationToken() {}
	
	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
	public NegotiateAuthorizationToken(String token) {
		update("Negotiate", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "Negotiate".equalsIgnoreCase(type);
	}
	

}
