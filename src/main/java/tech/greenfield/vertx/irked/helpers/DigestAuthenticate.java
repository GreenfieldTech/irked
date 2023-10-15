package tech.greenfield.vertx.irked.helpers;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.buffer.Buffer;
import tech.greenfield.vertx.irked.auth.ParameterEncodedAuthorizationToken;
import tech.greenfield.vertx.irked.status.Unauthorized;

/**
 * A 401 Unauthorized response that requests an HTTP Digest authorization for a specified realm.
 * 
 * Send this response instead of a 401 Unauthorized if you want the client to send an 
 * {@link DigestAuthenticate} in the next request.
 * 
 * The digest by default is using the common MD5 digest, though that can be customized, and the nonce is generated
 * using the SHA1PRNG {@link SecureRandom} algorithm.
 */
public class DigestAuthenticate extends Unauthorized {

	private static final long serialVersionUID = 1633008924546501215L;
	private static SecureRandom rand;
	
	static {
		try {
			rand = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) { // shouldn't happen - every JVM must have this implementation
			rand = null;
		}
	}
	
	/**
	 * Create a "digest authorization required" Unauthorized response
	 * @param realm Realm for which authorization should be provided
	 */
	public DigestAuthenticate(String realm) {
		this(realm, "");
	}

	/**
	 * Create a "digest authorization required" Unauthorized response with the specified opaque value 
	 * @param realm Realm for which authorization should be provided
	 * @param opaque the digest opaque value that should be returned by the client
	 */
	public DigestAuthenticate(String realm, String opaque) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toMillis(5)), opaque, "MD5", true);
	}

	/**
	 * Create a "digest authorization required" Unauthorized response, optionally disabling QoP authorization integrity.
	 * @param realm Realm for which authorization should be provided
	 * @param allowQopIntegrity whether to request QoP integrity
	 */
	public DigestAuthenticate(String realm, boolean allowQopIntegrity) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toSeconds(5)), "", "MD5", allowQopIntegrity);
	}

	/**
	 * Create a "digest authorization required" Unauthorized response with the specified opaque, and optionally disabling QoP
	 * authorization integrity.
	 * @param realm Realm for which authorization should be provided
	 * @param opaque the digest opaque value that should be returned by the client
	 * @param allowQopIntegrity whether to request QoP integrity
	 */
	public DigestAuthenticate(String realm, String opaque, boolean allowQopIntegrity) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toSeconds(5)), opaque, "MD5", allowQopIntegrity);
	}

	/**
	 * Create a "digest authorization required" Unauthorized response with a custom nonce, the specified opaque value,
	 * and a custom digest algorithm.
	 * @param realm Realm for which authorization should be provided
	 * @param nonce a custom nonce
	 * @param opaque the digest opaque value that should be returned by the client
	 * @param algorithm A digest algorithm to use instead of MD5
	 */
	public DigestAuthenticate(String realm, String nonce, String opaque, String algorithm) {
		this(realm, nonce, opaque, algorithm, true);
	}
	
	/**
	 * Create a "digest authorization required" Unauthorized response with a custom nonce, the specified opaque value,
	 * a custom digest algorithm, optionally disabling QoP authorization integrity.
	 * @param realm Realm for which authorization should be provided
	 * @param nonce a custom nonce
	 * @param opaque the digest opaque value that should be returned by the client
	 * @param algorithm A digest algorithm to use instead of MD5
	 * @param allowQopIntegrity whether to request QoP integrity
	 */
	public DigestAuthenticate(String realm, String nonce, String opaque, String algorithm, boolean allowQopIntegrity) {
		addHeader("WWW-Authenticate", "Digest " + Stream.of(
				"realm=\"" + realm + "\"",
				"qop=\"auth" + (allowQopIntegrity ? ", auth-int" : "") + "\"",
				"algorithm=\"" + algorithm + "\"",
				"nonce=\"" + nonce + "\"",
				"opaque=\"" + opaque + "\""
				).collect(Collectors.joining(", ")));
	}

	/**
	 * Helper method to generate an HTTP digest nonce.
	 * @param tag The nonce tag (supposedly the opaque value)
	 * @param expiry TTL of the nonce instead of the default 5 minutes
	 * @return a nonce for HTTP digest authorization
	 */
	public static String generateNonce(String tag, long expiry) {
		byte[] randdata = new byte[8];
		rand.nextBytes(randdata);
		String nonce = String.valueOf(System.currentTimeMillis() / 1000 + expiry) + ":" + tag + ":" + ParameterEncodedAuthorizationToken.toHex(randdata);
		return Base64.getEncoder().encodeToString(Buffer.buffer(nonce).getBytes());
	}

}
