package tech.greenfield.vertx.irked.auth;

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
