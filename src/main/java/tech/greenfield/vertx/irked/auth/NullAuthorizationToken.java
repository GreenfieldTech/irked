package tech.greenfield.vertx.irked.auth;

/**
 * An {@link AuthorizationToken} implementation to specify that no authorization token was present
 * @author odeda
 */
public class NullAuthorizationToken extends AuthorizationToken {

	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public NullAuthorizationToken() {
		update(null, null);
	}

}
