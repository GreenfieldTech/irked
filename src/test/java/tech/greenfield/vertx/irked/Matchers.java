package tech.greenfield.vertx.irked;

import java.util.Arrays;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class Matchers {
	protected static Matcher<HttpResponse<?>> isOK() {
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

	protected static Matcher<HttpResponse<?>> status(HttpError status) {
		return new BaseMatcher<HttpResponse<?>>() {
			@Override
			public boolean matches(Object actual) {
				return actual instanceof HttpResponse ?
						(((HttpResponse<?>)actual).statusCode() == status.getStatusCode()) : false;
			}
			
			@Override
			public void describeMismatch(Object item, Description description) {
				if (item instanceof HttpResponse) {
					var resp = (HttpResponse<?>)item;
					description.appendText("response has status HTTP ");
					description.appendText("" + resp.statusCode());
					description.appendText(" ");
					description.appendText(resp.statusMessage());
				} else
					description.appendText("value is not an HTTP Response (" + item.getClass() + ")");
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("response has status ");
				description.appendText(status.toString());
			}};
	}
	
	protected static Matcher<HttpResponse<Buffer>> bodyEmpty() {
		return new BaseMatcher<HttpResponse<Buffer>>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				if (actual instanceof HttpResponse) {
					var body = ((HttpResponse<Buffer>)actual).body();
					return body == null || body.length() == 0; // Vert.x 4 BodyCodecImpl sends empty bodies as null
				}
				return false;
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("is the response body empty");
			}};
	}
	
	protected static Matcher<HttpResponse<Buffer>> hasBody(String text) {
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
	
	protected static Matcher<HttpResponse<Buffer>> hasBody(byte[] data) {
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
	
	protected static Matcher<HttpResponse<Buffer>> bodyContains(String text) {
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
