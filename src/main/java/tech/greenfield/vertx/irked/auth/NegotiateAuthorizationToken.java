package tech.greenfield.vertx.irked.auth;

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
