package tech.greenfield.vertx.irked.auth;

import java.util.Objects;
import java.util.ServiceLoader;

public class AuthorizationToken {
	
	private String token;
	private String type;

	protected AuthorizationToken() {
	}
	
	public static AuthorizationToken parse(String authorizationHeader) {
		if (Objects.isNull(authorizationHeader))
			return new NullAuthorizationToken();
		String[] parts = authorizationHeader.split("\\s+",2);
		if (parts.length == 1)
			return new SimpleAuthrizationToken(parts[0]);
		for (AuthorizationToken a : ServiceLoader.load(AuthorizationToken.class)) {
			if (a.supports(parts[0]))
				return a.update(parts[0], parts[1]);
		}
		return new AuthorizationToken().update(parts[0], parts[1]);
	}

	protected AuthorizationToken update(String type, String token) {
		this.type = type;
		this.token = token;
		return this;
	}

	protected boolean supports(String type) {
		return false;
	}
	
	public boolean is(String type) {
		return Objects.isNull(type) ? Objects.isNull(this.type) : type.equalsIgnoreCase(this.type);
	}

	public String getToken() {
		return token;
	}

	public String getType() {
		return type;
	}
	
	public String toString() {
		return type + " " + token;
	}
}
