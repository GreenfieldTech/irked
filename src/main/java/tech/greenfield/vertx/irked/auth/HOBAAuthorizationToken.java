package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of the standard RFC 7486 Mutual authentication scheme token
 * 
 * The token parameters are available from the {@link ParameterEncodedAuthorizationToken#getParameter(String)}
 * method, though currently no validation is performed to make sure that all required fields are present.
 * 
 * @author odeda
 */
public class HOBAAuthorizationToken extends ParameterEncodedAuthorizationToken {

	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public HOBAAuthorizationToken() {}
	
	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
	public HOBAAuthorizationToken(String token) {
		update("HOBA", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "HOBA".equalsIgnoreCase(type);
	}
	
	/**
	 * Retrieve HOBA parameters
	 * @return a the HOBA parameter text sent by the client
	 */
	public String getResult() {
		return getParameter("result");
	}
}
