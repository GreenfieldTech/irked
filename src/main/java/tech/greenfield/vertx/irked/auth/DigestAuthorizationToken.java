package tech.greenfield.vertx.irked.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import tech.greenfield.vertx.irked.Request;
import tech.greenfield.vertx.irked.helpers.DigestAuthenticate;

/**
 * Implementation of the standard RFC 7616 Digest authentication scheme token
 * 
 * Complete implementation of token validation and data access is provided, depending on the
 * specified digest algorithm being available from the JVM cryptography provider.
 * 
 * @author odeda
 */
public class DigestAuthorizationToken extends ParameterEncodedAuthorizationToken {

	private static Logger logger = LoggerFactory.getLogger(DigestAuthorizationToken.class);

	private MessageDigest digestAlgorithm;

	public DigestAuthorizationToken() {}

	public DigestAuthorizationToken(String token) {
		update("Digest", token);
	}

	public DigestAuthorizationToken(String realm, String method, String uri, String username, String password,
			String nonce) {
		this(realm, method, uri, null, username, password, nonce, null, null, "MD5");
	}

	public DigestAuthorizationToken(String realm, String method, String uri, String username, String password,
			String nonce, String algorithm) {
		this(realm, method, uri, null, username, password, nonce, null, null, algorithm);
	}

	public DigestAuthorizationToken(String realm, String method, String uri, Buffer entityBody, String username, String password,
			String nonce, String cnonce) {
		this(realm, method, uri, entityBody, username, password, nonce, cnonce, null, "MD5");
	}

	public DigestAuthorizationToken(String realm, String method, String uri, Buffer entityBody, String username, String password,
			String nonce, String cnonce, String algorithm) {
		this(realm, method, uri, entityBody, username, password, nonce, cnonce, null, algorithm);
	}

	/**
	 * Helper constructor to compute a new Digest authorization header
	 * @param realm Realm received in the Unauthorized response
	 * @param method HTTP method to authorize
	 * @param uri URI of the request
	 * @param entityBody HTTP body content to authorize
	 * @param username Username to authenticate with
	 * @param password password to authentication with
	 * @param nonce Nonce received in the Unauthorized response
	 * @param cnonce cnonce received in the unauthorized response
	 * @param opaque whether the authorized content is opaque (as per the RFC)
	 * @param algorithm Algorithm to use
	 */
	public DigestAuthorizationToken(String realm, String method, String uri, Buffer entityBody, String username, String password,
			String nonce, String cnonce, String opaque, String algorithm) {
		parameters.put("username", username);
		parameters.put("realm", realm);
		parameters.put("nonce", nonce);
		parameters.put("uri", uri);
		if (Objects.nonNull(opaque))
			parameters.put("opaque", opaque);
		if (Objects.nonNull("cnonce")) {
			parameters.put("qop", Objects.isNull(entityBody) ? "auth" : "auth-int");
			parameters.put("nc", "00000001");
			parameters.put("cnonce", cnonce);
		} else {
			if (Objects.nonNull(entityBody))
				logger.warn("If entity body is provided for digest, cnonce must also be provided");
		}
		try {
			digestAlgorithm = MessageDigest.getInstance(algorithm);
			parameters.put("algorithm", algorithm);
		} catch (NoSuchAlgorithmException e) {
			parameters.put("algorithm", "unknown");
			return;
		}
		parameters.put("response", computeResponse(password, method, entityBody));
	}

	public String generateAuthrizationHeader() {
		return "Digest " + parameters.entrySet().stream()
				.map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
				.collect(Collectors.joining(", "));
	}

	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#supports(java.lang.String)
	 */
	@Override
	protected boolean supports(String type) {
		return "Digest".equalsIgnoreCase(type);
	}

	@Override
	protected AuthorizationToken update(String type, String token) {
		super.update(type, token);
		try {
			digestAlgorithm = MessageDigest.getInstance(getAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			logger.warn("Unsupported digest algorithm - " + e.getMessage());
			digestAlgorithm = null;
		}
		return this;
	}

	/**
	 * Check if the digest token is valid and additional test operations can work on it.
	 * If this method returns false, other validation methods are likely to fail.
	 *
	 * This method currently only checks that the specified digest algorithm is supported by the JVM.
	 * If you want to restrict the algorithm to only specific ones, use {@link #getAlgorithm()} to check.
	 * @return Whether the token is valid
	 */
	public boolean isValid() {
		return Objects.nonNull(digestAlgorithm);
	}

	/**
	 * Returns the digest algorithm claimed in the authorization token.
	 * @return Name of the digest algorithm
	 */
	public String getAlgorithm() {
		return Objects.nonNull(getParameter("algorithm")) ? getParameter("algorithm") : "MD5";
	}

	/**
	 * Retrieve the opaque value entrusted to the client in the challenge
	 * @return Opaque value if reported by the client, null otherwise
	 */
	public String getOpaque() {
		return getParameter("opaque");
	}

	/**
	 * Use the digest algorithm specified in the token to hash text according to RFC 7616
	 * @param text Text to hash
	 * @return a lowercased hex encoded hash of the provided text
	 */
	public String hash(String text) {
		return hash(Buffer.buffer(text));
	}

	/**
	 * Use the digest algorithm specified in the token to hash text according to RFC 7616
	 * @param buffer data to hash
	 * @return a lowercased hex encoded hash of the provided text
	 */
	public String hash(Buffer buffer) {
		if (!isValid())
			return "";
		return toHex(digestAlgorithm.digest(buffer.getBytes())).toLowerCase();
	}

	/**
	 * Retrieve the username claimed in the token
	 * @return username value of the token
	 */
	public String getUsername() {
		return getParameter("username");
	}

	/**
	 * Check if the sender requested a body integrity check
	 * @return whether the "qop" value of the digest specified "auth-int"
	 */
	public boolean qopIntegrityRequested() {
		return "auth-int".equals(getParameter("qop"));
	}

	/**
	 * Check if the response value provided in the token is valid considering the provided password, method
	 * and optional body.
	 *
	 * @param password Password to check against the digest response
	 * @param req HTTP request to verify integrity with. If the token has set "qop" to "auth-int"
	 * (as can be verified by {@link #qopIntegrityRequested()}, and a body is not provided in the request, this method
	 * will return false.
	 * @return Whether the response value specified in the token is correct according to RFC7616
	 */
	public boolean validateResponse(String password, Request req) {
		return getParameter("uri").equals(req.request().uri()) &&
				computeResponse(password, req.request().method().name(), req.getBody()).equals(getParameter("response"));
	}

	private String computeResponse(String password, String method, Buffer entityBody) {
		String A1 = hash(getParameter("username") + ":" + getParameter("realm") + ":" + password);
		String A2 = null;
		if(!parameters.containsKey("qop") || getParameter("qop").equals("auth"))
			A2 = hash(method + ":" + getParameter("uri"));
		else if(getParameter("qop").equals("auth-int") && Objects.nonNull(entityBody))
			A2 = hash(method + ":" + getParameter("uri") + hash(entityBody));
		else {
			logger.warn("Invalid digest format: " + getParameter("qop"));
			return "";
		}
		return parameters.containsKey("qop") ?
			hash(A1 + ":" + getParameter("nonce") + ":" + getParameter("nc") + ":" + getParameter("cnonce") + ":" +
					getParameter("qop") + ":" + A2)
			:
			hash(A1 + ":" + getParameter("nonce") + ":" + A2);
	}

	/**
	 * Check if the nonce is stale according to the nonce format suggested in RFC7616.
	 * @param duration seconds to allow for after the value specified in the nonce. IF the nonce was generated
	 * by {@link DigestAuthenticate}, set this value to 0, as the nonce generated by that helper class already
	 * specifies the maximum life of the nonce
	 * @return whether the nonce is stale
	 */
	public boolean isNonceStale(int duration) {
		String nonce = getParameter("nonce");
		try {
			String[] parts = new String(Base64.getDecoder().decode(nonce), "UTF-8").split(":");
			if (parts.length != 3)
				return true; // can't calculate staleness, so caution first
			long nonceTime = Long.parseLong(parts[0]);
			return (nonceTime + duration < (System.currentTimeMillis() / 1000));
		} catch (UnsupportedEncodingException e) { // shouldn't happen as UTF-8 is builtin
			return true;
		}
	}

}
