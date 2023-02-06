package tech.greenfield.vertx.irked.auth;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Objects;

/**
 * Base class for RFC 7235 authentication/authorization tokens
 * @author odeda
 */
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
			return new Iterable<>() {
				private TokenTypes[] types = values();
				@Override
				public Iterator<AuthorizationToken> iterator() {
					return new Iterator<>() {
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

	/**
	 * Constructor for token implementations is internal - use {@link #parse(String)} to create tokens
	 */
	protected AuthorizationToken() {
	}
	
	/**
	 * Parse an Authorization header text to create a specific token implementation
	 * 
	 * This method uses the convention of the first word in the Authorization value being the
	 * IANA HTTP Authentication Scheme Name, to dynamically load an appropriate implementation
	 * from the tech.greenfield.vertx.irked.auth.AuthorizationToken service loader. If the
	 * header value is incompatible (i.e. contains only one "word") then a token with the type
	 * "simple" will be returned. Otherwise if no supporting implementation is found, an instance
	 * of the {@link AuthorizationToken} class will be created.
	 * @param authorizationHeader value of the HTTP Authorization header
	 * @return a 
	 */
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

	/**
	 * Update a service-loader loaded implementation with the actual details
	 * @param type IANA HTTP Authentication Scheme Name
	 * @param token token text
	 * @return itself for fluent calling
	 */
	protected AuthorizationToken update(String type, String token) {
		this.type = type;
		this.token = token;
		return this;
	}

	/**
	 * Check whether an AuthorizationToken implementation supports the speciefied IANA HTTP Authentication Scheme Name
	 * @param type IANA HTTP Authentication Scheme Name
	 * @return whether this implementation supports the specified name
	 */
	protected boolean supports(String type) {
		return false;
	}
	
	/**
	 * Check whether the token implementation implements the specified IANA HTTP Authentication Scheme Name
	 * @param type IANA HTTP Authentication Scheme Name
	 * @return whether the implementation is valid for that scheme name
	 */
	public boolean is(String type) {
		return Objects.isNull(type) ? Objects.isNull(this.type) : type.equalsIgnoreCase(this.type);
	}

	/**
	 * Retrieve the authorization token text
	 * @return authorization token text
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Retrieve the IANA HTTP Authentication Scheme Name this token is an implementation of
	 * @return IANA HTTP Authentication Scheme Name
	 */
	public String getType() {
		return type;
	}
	
	public String toString() {
		return type + " " + token;
	}
}
