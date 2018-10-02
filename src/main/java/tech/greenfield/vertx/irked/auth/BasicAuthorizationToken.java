package tech.greenfield.vertx.irked.auth;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class BasicAuthorizationToken extends AuthorizationToken {

	private String username;
	private String password;
	
	public BasicAuthorizationToken() {}

	public BasicAuthorizationToken(String token) {
		update("Basic", token);
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "Basic".equalsIgnoreCase(type);
	}
	
	@Override
	protected AuthorizationToken update(String type, String token) {
		super.update(type, token);
		try {
			String[] parts = new String(Base64.getDecoder().decode(token), "UTF-8").split(":",2);
			username = parts[0];
			if (parts.length == 2)
				password = parts[1];
		} catch (UnsupportedEncodingException e) { // shouldn't happen, UTF-8 is builtin
		}
		return this;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

}
