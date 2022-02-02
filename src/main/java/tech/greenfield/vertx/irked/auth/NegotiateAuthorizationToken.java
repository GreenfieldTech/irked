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

	public NegotiateAuthorizationToken() {}
	
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
