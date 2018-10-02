package tech.greenfield.vertx.irked.auth;

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
