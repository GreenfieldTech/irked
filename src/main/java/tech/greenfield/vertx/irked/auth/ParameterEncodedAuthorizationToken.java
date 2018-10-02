package tech.greenfield.vertx.irked.auth;

import java.util.HashMap;
import java.util.Map;

public class ParameterEncodedAuthorizationToken extends AuthorizationToken {

	Map<String,String> parameters = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#update(java.lang.String, java.lang.String)
	 */
	@Override
	protected AuthorizationToken update(String type, String token) {
		super.update(type, token);
		for (String part : token.split(",\\s+")) {
			String[] parts = part.split("=",2);
			if (parts.length != 2)
				continue;
			if (parts[1].charAt(0) == '"')
				parts[1] = parts[1].substring(1, parts[1].length()-1);
			parameters.put(parts[0], parts[1]);
		}
		return this;
	}
	
	public String getParameter(String param) {
		return parameters.get(param);
	}
	
}
