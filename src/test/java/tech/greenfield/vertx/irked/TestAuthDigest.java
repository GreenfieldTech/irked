package tech.greenfield.vertx.irked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static tech.greenfield.vertx.irked.Matchers.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.auth.DigestAuthorizationToken;
import tech.greenfield.vertx.irked.auth.ParameterEncodedAuthorizationToken;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.helpers.DigestAuthenticate;
import tech.greenfield.vertx.irked.status.Unauthorized;

public class TestAuthDigest extends TestBase {
	
	public class TestController extends Controller {
		
		@Get("/auth")
		public void text(Request r) throws Unauthorized {
			if (!(r.getAuthorization() instanceof DigestAuthorizationToken)) {
				throw new DigestAuthenticate("irked", "opaque-value");
			}

			DigestAuthorizationToken auth = (DigestAuthorizationToken) r.getAuthorization();
			if (!auth.isValid() || auth.isNonceStale(0) || !auth.validateResponse(userPass, r)) {
				throw new DigestAuthenticate("irked", "opaque-value");
			}
			r.sendContent("OK");
		}
		
		@Post("/auth-int")
		BodyHandler bodyHandler = BodyHandler.create();
		
		@Post("/auth-int")
		public void binary(Request r) throws Unauthorized {
			if (!(r.getAuthorization() instanceof DigestAuthorizationToken)) {
				throw new DigestAuthenticate("irked", "opaque-value");
			}

			DigestAuthorizationToken auth = (DigestAuthorizationToken) r.getAuthorization();
			if (!auth.isValid() || auth.isNonceStale(0) || !auth.validateResponse(userPass, r)) {
				throw new DigestAuthenticate("irked", "opaque-value");
			}
			r.sendContent("OK");
		};
		
		@OnFail
		@Endpoint("/*")
		WebHandler failure = Request.failureHandler();
	}

	private static final String userPass = "password";
	private static final String userName = "user";
	private Buffer postdata = Buffer.buffer("data");

	@BeforeEach
	public void deployServer(VertxTestContext context, Vertx vertx) {
		deployController(new TestController(), vertx, context.succeedingThenComplete());
	}

	@Test
	public void testGetAuthed(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).get(port, "localhost", "/auth").sendP()
		.thenCompose(res -> testGetAuthedAuth(vertx, res))
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	private CompletableFuture<Void> testGetAuthedAuth(Vertx vertx, HttpResponse<Buffer> res) {
		assertThat(res, is(status(new Unauthorized())));
		try {
			Map<String, String> auth = parseAuthHeader(res.getHeader("WWW-Authenticate"));
			DigestAuthorizationToken tok = new DigestAuthorizationToken(auth.get("realm"), "GET", "/auth", userName, userPass, auth.get("nonce"));
			return getClient(vertx).get(port, "localhost", "/auth")
			.putHeader("Authorization", tok.generateAuthrizationHeader())
			.sendP()
			.thenAccept(res2 -> testGetAuthedCheckResponse(res2));
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	private void testGetAuthedCheckResponse(HttpResponse<Buffer> res) {
		assertThat(res, isOK());
		assertThat(res.toString(), equalTo("OK"));
	}

	@Test
	public void testPostAuthed(VertxTestContext context, Vertx vertx) {
		Checkpoint async = context.checkpoint();
		getClient(vertx).post(port, "localhost", "/auth-int")
		.putHeader("Content-Length", String.valueOf(postdata.length()))
		.sendP(postdata)
		.thenCompose(res -> testPostAuthedAuth(res, vertx))
		.exceptionally(failureHandler(context))
		.thenRun(async::flag);
	}

	private CompletableFuture<Void> testPostAuthedAuth(HttpResponse<Buffer> res, Vertx vertx) {
		assertThat(res, is(status(new Unauthorized())));
		try {
			Map<String, String> auth = parseAuthHeader(res.getHeader("WWW-Authenticate"));
			DigestAuthorizationToken tok = new DigestAuthorizationToken(auth.get("realm"), "POST", "/auth-int", postdata, userName, userPass, auth.get("nonce"), DigestAuthenticate.generateNonce("", 300));
			return getClient(vertx).post(port, "localhost", "/auth-int")
			.putHeader("Authorization", tok.generateAuthrizationHeader())
			.putHeader("Content-Length", String.valueOf(postdata.length()))
			.sendP(postdata)
			.thenAccept(res2 -> testGetAuthedCheckResponse(res2));
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	private Map<String,String> parseAuthHeader(String authHeader) throws Exception {
		String[] authparts = authHeader.split(" ",2);
		if (!"Digest".equals(authparts[0]) || authparts.length != 2)
			throw new Exception("Unexpected auth type: " + authparts[0]);
		System.err.println("Authorization header: " + authHeader);
		return StreamSupport.stream(ParameterEncodedAuthorizationToken.parseParameters(authparts[1]).spliterator(), false)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
