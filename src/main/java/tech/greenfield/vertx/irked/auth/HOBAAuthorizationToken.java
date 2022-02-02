package tech.greenfield.vertx.irked.auth;

/**
 * Implementation of the standard RFC 7486 Mututal authentication scheme token
 * 
 * The token parameters are available from the {@link ParameterEncodedAuthorizationToken#getParameter(String)}
 * method, though currently no validation is performed to make sure that all required fields are present.
 * 
 * @author odeda
 */
public class HOBAAuthorizationToken extends ParameterEncodedAuthorizationToken {

	public HOBAAuthorizationToken() {}
	
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
	
	public String getResult() {
		return getParameter("result");
	}
}
