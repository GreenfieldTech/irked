package tech.greenfield.vertx.irked.auth;

public class OAuthAuthorizationToken extends ParameterEncodedAuthorizationToken {

	public OAuthAuthorizationToken() {}
	
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
