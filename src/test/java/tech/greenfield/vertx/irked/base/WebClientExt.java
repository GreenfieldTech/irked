package tech.greenfield.vertx.irked.base;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.WebClientBase;

public class WebClientExt extends WebClientBase {

	private WebSocketClient websocket;

	public WebClientExt(Vertx vertx, WebClientOptions options) {
		super(vertx.createHttpClient(), options);
		websocket = vertx.createWebSocketClient();
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
	
	public Future<WebSocket> websocket(int port, String host, String requestURI) {
		return websocket.connect(port, host, requestURI);
	}

	public Future<WebSocket> websocket(int port, String host, String requestURI, HeadersMultiMap headers) {
		return websocket.connect(new WebSocketConnectOptions()
				.setPort(port)
				.setHost(host)
				.setURI(requestURI)
				.setHeaders(headers));
	}
}
