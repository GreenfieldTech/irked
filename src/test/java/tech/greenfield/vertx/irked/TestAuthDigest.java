package tech.greenfield.vertx.irked;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.handler.BodyHandler;
import tech.greenfield.vertx.irked.annotations.*;
import tech.greenfield.vertx.irked.auth.DigestAuthorizationToken;
import tech.greenfield.vertx.irked.base.TestBase;
import tech.greenfield.vertx.irked.helpers.DigestAuthenticate;
import tech.greenfield.vertx.irked.status.OK;
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

	@Before
	public void deployServer(TestContext context) {
		deployController(new TestController(), context.asyncAssertSuccess());
	}

	@Test
	public void testGetAuthed(TestContext context) {
		Async async = context.async();
		getClient().get(port, "localhost", "/auth").exceptionHandler(t -> context.fail(t))
		.handler(res -> testGetAuthedAuth(context, res, async::complete)).end();
	}
	
	private void testGetAuthedAuth(TestContext ctx, HttpClientResponse res, Runnable finish) {
		ctx.assertEquals(Unauthorized.code, res.statusCode());
		try {
			Map<String, String> auth = parseAuthHeader(res.getHeader("WWW-Authenticate"));
			DigestAuthorizationToken tok = new DigestAuthorizationToken(auth.get("realm"), "GET", "/auth", userName, userPass, auth.get("nonce"));
			getClient().get(port, "localhost", "/auth").exceptionHandler(ctx::fail)
			.handler(res2 -> testGetAuthedCheckResponse(ctx, res2, finish))
			.putHeader("Authorization", tok.generateAuthrizationHeader())
			.end();
		} catch (Exception e) {
			ctx.fail(e);
		}
	}

	private void testGetAuthedCheckResponse(TestContext ctx, HttpClientResponse res, Runnable finish) {
		ctx.assertEquals(OK.code, res.statusCode());
		res.bodyHandler(body -> {
			ctx.assertEquals("OK", body.toString());
			finish.run();
		});
	}
	
	@Test
	public void testPostAuthed(TestContext context) {
		Async async = context.async();
		getClient().post(port, "localhost", "/auth-int").exceptionHandler(t -> context.fail(t))
		.handler(res -> testPostAuthedAuth(context, res, async::complete))
		.putHeader("Content-Length", String.valueOf(postdata.length()))
		.write(postdata).end();
	}

	
	private void testPostAuthedAuth(TestContext ctx, HttpClientResponse res, Runnable finish) {
		ctx.assertEquals(Unauthorized.code, res.statusCode());
		try {
			Map<String, String> auth = parseAuthHeader(res.getHeader("WWW-Authenticate"));
			DigestAuthorizationToken tok = new DigestAuthorizationToken(auth.get("realm"), "POST", "/auth-int", postdata, userName, userPass, auth.get("nonce"), DigestAuthenticate.generateNonce("", 300));
			getClient().post(port, "localhost", "/auth-int").exceptionHandler(ctx::fail)
			.handler(res2 -> testGetAuthedCheckResponse(ctx, res2, finish))
			.putHeader("Authorization", tok.generateAuthrizationHeader())
			.putHeader("Content-Length", String.valueOf(postdata.length()))
			.write(postdata).end();
		} catch (Exception e) {
			ctx.fail(e);
		}
	}

	private Map<String,String> parseAuthHeader(String authHeader) throws Exception {
		String[] authparts = authHeader.split(" ",2);
		if (!"Digest".equals(authparts[0]) || authparts.length != 2)
			throw new Exception("Unexpected auth type: " + authparts[0]);
		return Arrays.asList(authparts[1].split(",\\s+")).stream()
				.map(s -> s.split("=",2))
				.map(ss -> {
					if (ss[1].charAt(0) == '"')
						ss[1] = ss[1].substring(1, ss[1].length()-1);
					return ss;
				})
				.collect(Collectors.toMap(ss -> ss[0], ss -> ss[1]));
	}
}
