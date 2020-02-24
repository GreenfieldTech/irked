package tech.greenfield.vertx.irked.base;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import tech.greenfield.vertx.irked.Controller;
import tech.greenfield.vertx.irked.HttpError;
import tech.greenfield.vertx.irked.server.Server;

public class TestBase {

	private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 * 1024;

	@RegisterExtension
	static VertxExtension vertxExtension = new VertxExtension();
	protected final Integer port = new Random().nextInt(30000)+10000;

	protected WebClientExt getClient(Vertx vertx) {
		return new WebClientExt(vertx, new WebClientOptions(new HttpClientOptions()
				.setIdleTimeout(0)
				.setMaxWebsocketMessageSize(MAX_WEBSOCKET_MESSAGE_SIZE)));
	}

	protected void deployController(Controller controller, Vertx vertx, Handler<AsyncResult<String>> handler) {
		Server server = new Server(controller);

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", port));
		vertx.deployVerticle(server, options, handler);
	}

	protected Function<Throwable, Void> failureHandler(VertxTestContext context) {
		return  t -> {
			context.failNow(t);
			return null;
		};
	}

	protected Matcher<HttpResponse<?>> isOK() {
		return new BaseMatcher<HttpResponse<?>>() {
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						(((HttpResponse<?>)actual).statusCode() / 100 == 2) : false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response indicating success");
			}};
	}

	protected Matcher<HttpResponse<?>> status(HttpError status) {
		return new BaseMatcher<HttpResponse<?>>() {
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						(((HttpResponse<?>)actual).statusCode() == status.getStatusCode()) : false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("is the response of status");
				description.appendValue(status);
			}};
	}
	
	protected Matcher<HttpResponse<Buffer>> bodyEmpty() {
		return new BaseMatcher<HttpResponse<Buffer>>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						(((HttpResponse<Buffer>)actual).body().length() == 0) : false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response body empty");
			}};
	}
	
	protected Matcher<HttpResponse<Buffer>> hasBody(String text) {
		return new BaseMatcher<HttpResponse<Buffer>>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						((HttpResponse<Buffer>)actual).bodyAsString().equals(text) : false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response body empty");
			}};
	}
	
	protected Matcher<HttpResponse<Buffer>> hasBody(byte[] data) {
		return new BaseMatcher<HttpResponse<Buffer>>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						Arrays.equals(((HttpResponse<Buffer>)actual).body().getBytes(), data) : false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response body empty");
			}};
	}
	
	protected Matcher<HttpResponse<Buffer>> bodyContains(String text) {
		return new BaseMatcher<HttpResponse<Buffer>>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						((HttpResponse<Buffer>)actual).bodyAsString().contains(text) : false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response body empty");
			}};
	}

}
