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
	
	public DigestAuthenticate(String realm) {
		this(realm, "");
	}

	public DigestAuthenticate(String realm, String opaque) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toMillis(5)), opaque, "MD5", true);
	}

	public DigestAuthenticate(String realm, boolean allowQopIntegrity) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toSeconds(5)), "", "MD5", allowQopIntegrity);
	}

	public DigestAuthenticate(String realm, String opaque, boolean allowQopIntegrity) {
		this(realm, generateNonce("", TimeUnit.MINUTES.toSeconds(5)), opaque, "MD5", allowQopIntegrity);
	}

	public DigestAuthenticate(String realm, String nonce, String opaque, String algorithm) {
		this(realm, nonce, opaque, algorithm, true);
	}
	
	public DigestAuthenticate(String realm, String nonce, String opaque, String algorithm, boolean allowQopIntegrity) {
		addHeader("WWW-Authenticate", "Digest " + Stream.of(
				"realm=\"" + realm + "\"",
				"qop=\"auth" + (allowQopIntegrity ? ", auth-int" : "") + "\"",
				"algorithm=\"" + algorithm + "\"",
				"nonce=\"" + nonce + "\"",
				"opaque=\"" + opaque + "\""
				).collect(Collectors.joining(", ")));
	}

	public static String generateNonce(String tag, long expiry) {
		byte[] randdata = new byte[8];
		rand.nextBytes(randdata);
		String nonce = String.valueOf(System.currentTimeMillis() / 1000 + expiry) + ":" + tag + ":" + ParameterEncodedAuthorizationToken.toHex(randdata);
		return Base64.getEncoder().encodeToString(Buffer.buffer(nonce).getBytes());
	}

}
