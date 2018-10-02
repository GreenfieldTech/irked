package tech.greenfield.vertx.irked.auth;

public class MutualAuthorizationToken extends ParameterEncodedAuthorizationToken {

	public MutualAuthorizationToken() {}
	
	public MutualAuthorizationToken(String token) {
		update("Mutual", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "Mutual".equalsIgnoreCase(type);
	}
	

}
