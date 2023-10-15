package tech.greenfield.vertx.irked.auth;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

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
	
	/**
	 * Create a new empty token for parsing incoming requests
	 */
	public BasicAuthorizationToken() {}

	/**
	 * Create a new token with the provided content
	 * @param token the authorization token text
	 */
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
		} catch (IllegalArgumentException e) { // invalid Base64 text - possibly some kind of abuse
			username = password = ""; // ignore the token and assume empty fields
		}
		return this;
	}
	
	/**
	 * Retrieve the parsed user name from the authorization token
	 * @return authorizing user name
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Retrieve the parsed password from the authorization token
	 * @return authorizing user's password
	 */
	public String getPassword() {
		return password;
	}

}
