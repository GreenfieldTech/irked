package tech.greenfield.vertx.irked.auth;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import tech.greenfield.vertx.irked.status.BadRequest;

/**
 * Implementation of the standard RFC 7617 Bearer authentication scheme token
 * 
 * This implementation parses the token and makes the user name and password available through the API.
 * 
 * @author odeda
 */
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
	/**
	 * This implementation throws a {@link BadRequest} if the Basic authorization value cannot be decoded
	 */
	protected AuthorizationToken update(String type, String token) {
		super.update(type, token);
		try {
			String[] parts = new String(Base64.getDecoder().decode(token), "UTF-8").split(":",2);
			username = parts[0];
			if (parts.length == 2)
				password = parts[1];
		} catch (UnsupportedEncodingException e) { // shouldn't happen, UTF-8 is builtin
		} catch (IllegalArgumentException e) { // invalid Base64 text - possibly some kind of abuse
			throw new BadRequest("Invalid Basic auth value").unchecked();
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
