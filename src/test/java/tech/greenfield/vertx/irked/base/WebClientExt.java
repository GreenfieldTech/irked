package tech.greenfield.vertx.irked.base;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.WebClientBase;

public class WebClientExt extends WebClientBase {

	private static HttpClient client;

	public WebClientExt(Vertx vertx, WebClientOptions options) {
		super(client = vertx.createHttpClient(), options);
	}
	
	@Override
	public HttpRequestExt<Buffer> get(int port, String host, String requestURI) {
		return new HttpRequestExt<Buffer>(super.get(port, host, requestURI));
	}

	@Override
	public HttpRequestExt<Buffer> post(int port, String host, String requestURI) {
		return new HttpRequestExt<Buffer>(super.post(port, host, requestURI));
	}
	
	@Override
	public HttpRequestExt<Buffer> put(int port, String host, String requestURI) {
		return new HttpRequestExt<Buffer>(super.put(port, host, requestURI));
	}

	@Override
	public HttpRequestExt<Buffer> patch(int port, String host, String requestURI) {
		return new HttpRequestExt<Buffer>(super.patch(port, host, requestURI));
	}

	@Override
	public HttpRequestExt<Buffer> delete(int port, String host, String requestURI) {
		return new HttpRequestExt<Buffer>(super.delete(port, host, requestURI));
	}
	
	public CompletableFuture<WebSocket> websocket(int port, String host, String requestURI) {
		CompletableFuture<WebSocket> fut = new CompletableFuture<>();
		client.webSocket(port, host, requestURI, res -> {
			if (res.failed()) fut.completeExceptionally(res.cause());
			else fut.complete(res.result());
		});
		return fut;
	}

	public CompletableFuture<WebSocket> websocket(int port, String host, String requestURI, HeadersMultiMap headers) {
		CompletableFuture<WebSocket> fut = new CompletableFuture<>();
		client.webSocket(new WebSocketConnectOptions()
				.setPort(port)
				.setHost(host)
				.setURI(requestURI)
				.setHeaders(headers), res -> {
			if (res.failed()) fut.completeExceptionally(res.cause());
			else fut.complete(res.result());
		});
		return fut;
	}
}
