
package tech.greenfield.vertx.irked.base;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.client.predicate.ResponsePredicateResult;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.uritemplate.Variables;

public class HttpRequestExt<T> implements HttpRequest<T> {

	private HttpRequest<T> httpRequest;

	public HttpRequestExt(HttpRequest<T> httpRequest) {
		this.httpRequest = httpRequest;
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#method(io.vertx.core.http.HttpMethod)
	 */
	@Override
	public HttpRequest<T> method(HttpMethod value) {
		return httpRequest.method(value);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#port(int)
	 */
	@Override
	public HttpRequest<T> port(int value) {
		return httpRequest.port(value);
	}

	/**
	 * @param <U>
	 * @param responseCodec
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#as(io.vertx.ext.web.codec.BodyCodec)
	 */
	@Override
	public <U> HttpRequest<U> as(BodyCodec<U> responseCodec) {
		return httpRequest.as(responseCodec);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#host(java.lang.String)
	 */
	@Override
	public HttpRequest<T> host(String value) {
		return httpRequest.host(value);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#virtualHost(java.lang.String)
	 */
	@Override
	public HttpRequest<T> virtualHost(String value) {
		return httpRequest.virtualHost(value);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#uri(java.lang.String)
	 */
	@Override
	public HttpRequest<T> uri(String value) {
		return httpRequest.uri(value);
	}

	/**
	 * @param headers
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#putHeaders(io.vertx.core.MultiMap)
	 */
	@Override
	public HttpRequest<T> putHeaders(MultiMap headers) {
		return httpRequest.putHeaders(headers);
	}

	/**
	 * @param name
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#putHeader(java.lang.String, java.lang.String)
	 */
	@Override
	public HttpRequestExt<T> putHeader(String name, String value) {
		httpRequest.putHeader(name, value);
		return this;
	}

	/**
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#headers()
	 */
	@Override
	public MultiMap headers() {
		return httpRequest.headers();
	}

	/**
	 * @param id
	 * @param password
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#basicAuthentication(java.lang.String, java.lang.String)
	 */
	@Override
	public HttpRequest<T> basicAuthentication(String id, String password) {
		return httpRequest.basicAuthentication(id, password);
	}

	/**
	 * @param id
	 * @param password
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#basicAuthentication(io.vertx.core.buffer.Buffer, io.vertx.core.buffer.Buffer)
	 */
	@Override
	public HttpRequest<T> basicAuthentication(Buffer id, Buffer password) {
		return httpRequest.basicAuthentication(id, password);
	}

	/**
	 * @param bearerToken
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#bearerTokenAuthentication(java.lang.String)
	 */
	@Override
	public HttpRequest<T> bearerTokenAuthentication(String bearerToken) {
		return httpRequest.bearerTokenAuthentication(bearerToken);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#ssl(java.lang.Boolean)
	 */
	@Override
	public HttpRequest<T> ssl(Boolean value) {
		return httpRequest.ssl(value);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#timeout(long)
	 */
	@Override
	public HttpRequest<T> timeout(long value) {
		return httpRequest.timeout(value);
	}

	/**
	 * @param paramName
	 * @param paramValue
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#addQueryParam(java.lang.String, java.lang.String)
	 */
	@Override
	public HttpRequest<T> addQueryParam(String paramName, String paramValue) {
		return httpRequest.addQueryParam(paramName, paramValue);
	}

	/**
	 * @param paramName
	 * @param paramValue
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#setQueryParam(java.lang.String, java.lang.String)
	 */
	@Override
	public HttpRequest<T> setQueryParam(String paramName, String paramValue) {
		return httpRequest.setQueryParam(paramName, paramValue);
	}

	/**
	 * @param value
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#followRedirects(boolean)
	 */
	@Override
	public HttpRequest<T> followRedirects(boolean value) {
		return httpRequest.followRedirects(value);
	}

	/**
	 * @param predicate
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#expect(java.util.function.Function)
	 */
	@Override
	public HttpRequest<T> expect(Function<HttpResponse<Void>, ResponsePredicateResult> predicate) {
		return httpRequest.expect(predicate);
	}

	/**
	 * @param predicate
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#expect(io.vertx.ext.web.client.predicate.ResponsePredicate)
	 */
	@Override
	public HttpRequest<T> expect(ResponsePredicate predicate) {
		return httpRequest.expect(predicate);
	}

	/**
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#queryParams()
	 */
	@Override
	public MultiMap queryParams() {
		return httpRequest.queryParams();
	}

	/**
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#copy()
	 */
	@Override
	public HttpRequest<T> copy() {
		return httpRequest.copy();
	}

	/**
	 * @param allow
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#multipartMixed(boolean)
	 */
	@Override
	public HttpRequest<T> multipartMixed(boolean allow) {
		return httpRequest.multipartMixed(allow);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendStream(io.vertx.core.streams.ReadStream, io.vertx.core.Handler)
	 */
	@Override
	public void sendStream(ReadStream<Buffer> body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendStream(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendStream(io.vertx.core.streams.ReadStream)
	 */
	@Override
	public Future<HttpResponse<T>> sendStream(ReadStream<Buffer> body) {
		return httpRequest.sendStream(body);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendBuffer(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	@Override
	public void sendBuffer(Buffer body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendBuffer(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendBuffer(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Future<HttpResponse<T>> sendBuffer(Buffer body) {
		return httpRequest.sendBuffer(body);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendJsonObject(io.vertx.core.json.JsonObject, io.vertx.core.Handler)
	 */
	@Override
	public void sendJsonObject(JsonObject body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendJsonObject(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendJsonObject(io.vertx.core.json.JsonObject)
	 */
	@Override
	public Future<HttpResponse<T>> sendJsonObject(JsonObject body) {
		return httpRequest.sendJsonObject(body);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendJson(java.lang.Object, io.vertx.core.Handler)
	 */
	@Override
	public void sendJson(Object body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendJson(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendJson(java.lang.Object)
	 */
	@Override
	public Future<HttpResponse<T>> sendJson(Object body) {
		return httpRequest.sendJson(body);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendForm(io.vertx.core.MultiMap, io.vertx.core.Handler)
	 */
	@Override
	public void sendForm(MultiMap body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendForm(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendForm(io.vertx.core.MultiMap)
	 */
	@Override
	public Future<HttpResponse<T>> sendForm(MultiMap body) {
		return httpRequest.sendForm(body);
	}

	/**
	 * @param body
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#sendMultipartForm(io.vertx.ext.web.multipart.MultipartForm, io.vertx.core.Handler)
	 */
	@Override
	public void sendMultipartForm(MultipartForm body, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendMultipartForm(body, handler);
	}

	/**
	 * @param body
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#sendMultipartForm(io.vertx.ext.web.multipart.MultipartForm)
	 */
	@Override
	public Future<HttpResponse<T>> sendMultipartForm(MultipartForm body) {
		return httpRequest.sendMultipartForm(body);
	}

	/**
	 * @param handler
	 * @see io.vertx.ext.web.client.HttpRequest#send(io.vertx.core.Handler)
	 */
	@Override
	public void send(Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.send(handler);
	}

	/**
	 * @return
	 * @see io.vertx.ext.web.client.HttpRequest#send()
	 */
	@Override
	public Future<HttpResponse<T>> send() {
		return httpRequest.send();
	}

	public Future<HttpResponse<T>> send(String body) {
		return sendBuffer(Buffer.buffer(body));
	}

	public Future<HttpResponse<T>> send(Buffer body) {
		return sendBuffer(body);
	}
	
	public Future<HttpResponse<T>> send(JsonObject body) {
		return sendJsonObject(body);
	}
	
	@Override
	public HttpRequest<T> putHeader(String name, Iterable<String> value) {
		httpRequest.putHeader(name, value);
		return this;
	}

	@Override
	public HttpRequest<T> authentication(Credentials credentials) {
		httpRequest.authentication(credentials);
		return this;
	}

	@Override
	public void sendForm(MultiMap body, String charset, Handler<AsyncResult<HttpResponse<T>>> handler) {
		httpRequest.sendForm(body, charset, handler);
	}

	@Override
	public HttpRequest<T> proxy(ProxyOptions proxyOptions) {
		httpRequest.proxy(proxyOptions);
		return this;
	}

	@Override
	public HttpRequest<T> setTemplateParam(String paramName, String paramValue) {
		httpRequest.setTemplateParam(paramName, paramValue);
		return this;
	}

	@Override
	public HttpRequest<T> setTemplateParam(String paramName, List<String> paramValue) {
		httpRequest.setTemplateParam(paramName, paramValue);
		return this;
	}

	@Override
	public HttpRequest<T> setTemplateParam(String paramName, Map<String, String> paramValue) {
		httpRequest.setTemplateParam(paramName, paramValue);
		return this;
	}

	@Override
	public Variables templateParams() {
		return httpRequest.templateParams();
	}

	@Override
	public String traceOperation() {
		return httpRequest.traceOperation();
	}
	
	@Override
	public HttpRequest<T> traceOperation(String traceOperation) {
		httpRequest.traceOperation(traceOperation);
		return this;
	}

	public HttpMethod method() {
		return httpRequest.method();
	}

	public int port() {
		return httpRequest.port();
	}

	public BodyCodec<T> bodyCodec() {
		return httpRequest.bodyCodec();
	}

	public String host() {
		return httpRequest.host();
	}

	public String virtualHost() {
		return httpRequest.virtualHost();
	}

	public String uri() {
		return httpRequest.uri();
	}

	public Boolean ssl() {
		return httpRequest.ssl();
	}

	public boolean followRedirects() {
		return httpRequest.followRedirects();
	}

	public ProxyOptions proxy() {
		return httpRequest.proxy();
	}

	public List<ResponsePredicate> expectations() {
		return httpRequest.expectations();
	}

	public boolean multipartMixed() {
		return httpRequest.multipartMixed();
	}

	public Future<HttpResponse<T>> sendForm(MultiMap body, String charset) {
		return httpRequest.sendForm(body, charset);
	}

	@Override
	public long timeout() {
		return httpRequest.timeout();
	}

	@Override
	public HttpRequest<T> idleTimeout(long timeout) {
		return httpRequest.idleTimeout(timeout);
	}

	@Override
	public long idleTimeout() {
		return httpRequest.idleTimeout();
	}

	@Override
	public HttpRequest<T> connectTimeout(long timeout) {
		return httpRequest.connectTimeout(timeout);
	}

	@Override
	public long connectTimeout() {
		return httpRequest.connectTimeout();
	}

}
