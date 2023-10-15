package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of the standard RFC 5849 OAuth authentication scheme token.
 * 
 * The token parameters are available from the {@link ParameterEncodedAuthorizationToken#getParameter(String)}
 * method, though currently no validation is performed to make sure that all required fields are present.
 * 
 * @author odeda
 */
public class OAuthAuthorizationToken extends ParameterEncodedAuthorizationToken {

	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public OAuthAuthorizationToken() {}

	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
	public OAuthAuthorizationToken(String token) {
		update("OAuth", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "OAuth".equalsIgnoreCase(type);
	}
	

}
