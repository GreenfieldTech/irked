package tech.greenfield.vertx.irked.auth;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Objects;

public class AuthorizationToken {
	
	private String token;
	private String type;
	
	public enum TokenTypes {
		// not actually a type - just a base implementation:
		// PARAMETER(ParameterEncodedAuthorizationToken.class)
		
		NULL(NullAuthorizationToken.class),
		SIMPLE(SimpleAuthrizationToken.class),
		BASIC(BasicAuthorizationToken.class),
		BEARER(BearerAuthorizationToken.class),
		DIGEST(DigestAuthorizationToken.class),
		HOB(HOBAAuthorizationToken.class),
		MUTUAL(MutualAuthorizationToken.class),
		NEGOTIATE(NegotiateAuthorizationToken.class),
		OAUTH(OAuthAuthorizationToken.class);

		private Class<? extends AuthorizationToken> clz;

		TokenTypes(Class<? extends AuthorizationToken> clz) {
			this.clz = clz;
		}

		public static Iterable<AuthorizationToken> instances() {
			return new Iterable<AuthorizationToken>() {
				private TokenTypes[] types = values();
				@Override
				public Iterator<AuthorizationToken> iterator() {
					return new Iterator<AuthorizationToken>() {
						private int index = 0;
						private int max = types.length;
						@Override
						public boolean hasNext() {
							return index < max;
						}
						@Override
						public AuthorizationToken next() {
							try {
								return types[index++].clz.getConstructor().newInstance();
							} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
									| InvocationTargetException | NoSuchMethodException | SecurityException e) {
								System.err.print("Unexpected exception instantiating a known authorization token type! this shouldn't happen");
								throw new Error(e);
							}
						}
					};
				}};
		}
	}

	protected AuthorizationToken() {
	}
	
	public static AuthorizationToken parse(String authorizationHeader) {
		if (Objects.isNull(authorizationHeader))
			return new NullAuthorizationToken();
		String[] parts = authorizationHeader.split("\\s+",2);
		if (parts.length == 1)
			return new SimpleAuthrizationToken(parts[0]);
		for (AuthorizationToken a : TokenTypes.instances()) {
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
