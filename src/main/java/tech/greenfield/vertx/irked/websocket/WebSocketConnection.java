package tech.greenfield.vertx.irked.websocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.net.SocketAddress;
import tech.greenfield.vertx.irked.Request;

public class WebSocketConnection implements ServerWebSocket {

	private ServerWebSocket socket;
	private Request request;
	
	public WebSocketConnection(Request request) {
		this.request = request;
		socket = request.request().upgrade();
	}
	
	public Request request() {
		return request;
	}
	
	public WebSocketConnection messageHandler(Handler<? super WebSocketMessage> handler) {
		socket.binaryMessageHandler(buffer -> handler.handle(new WebSocketMessage(request, socket, buffer)));
		// The text message handler is more expensive because it effectively causes UTF-8 decoding + encoding + (probably) final decoding
		// we should implement a custom frame aggregator that allows us to delay decoding text until the user actually wants to.  
		socket.textMessageHandler(text -> handler.handle(new WebSocketMessage(request, socket, text)));
		return this;
	}

	public ServerWebSocket exceptionHandler(Handler<Throwable> handler) {
		return socket.exceptionHandler(handler);
	}

	public ServerWebSocket handler(Handler<Buffer> handler) {
		return socket.handler(handler);
	}

	public ServerWebSocket pause() {
		return socket.pause();
	}

	public ServerWebSocket resume() {
		return socket.resume();
	}

	public ServerWebSocket endHandler(Handler<Void> endHandler) {
		return socket.endHandler(endHandler);
	}

	public ServerWebSocket write(Buffer data) {
		return socket.write(data);
	}

	public ServerWebSocket setWriteQueueMaxSize(int maxSize) {
		return socket.setWriteQueueMaxSize(maxSize);
	}

	public ServerWebSocket drainHandler(Handler<Void> handler) {
		return socket.drainHandler(handler);
	}

	public ServerWebSocket writeFrame(WebSocketFrame frame) {
		return socket.writeFrame(frame);
	}

	public ServerWebSocket writeFinalTextFrame(String text) {
		return socket.writeFinalTextFrame(text);
	}

	public ServerWebSocket writeFinalBinaryFrame(Buffer data) {
		return socket.writeFinalBinaryFrame(data);
	}

	public String binaryHandlerID() {
		return socket.binaryHandlerID();
	}

	public ServerWebSocket writeBinaryMessage(Buffer data) {
		return socket.writeBinaryMessage(data);
	}

	public ServerWebSocket closeHandler(Handler<Void> handler) {
		return socket.closeHandler(handler);
	}

	public ServerWebSocket frameHandler(Handler<WebSocketFrame> handler) {
		return socket.frameHandler(handler);
	}

	public String uri() {
		return socket.uri();
	}

	public String path() {
		return socket.path();
	}

	public String query() {
		return socket.query();
	}

	public MultiMap headers() {
		return socket.headers();
	}

	public String textHandlerID() {
		return socket.textHandlerID();
	}

	public void accept() {
		socket.accept();
	}

	public boolean writeQueueFull() {
		return socket.writeQueueFull();
	}

	public void reject() {
		socket.reject();
	}

	public String subProtocol() {
		return socket.subProtocol();
	}

	public void reject(int status) {
		socket.reject(status);
	}

	public SSLSession sslSession() {
		return socket.sslSession();
	}

	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return socket.peerCertificateChain();
	}

	public WebSocketBase writeTextMessage(String text) {
		return socket.writeTextMessage(text);
	}

	public WebSocketBase writePing(Buffer data) {
		return socket.writePing(data);
	}

	public WebSocketBase writePong(Buffer data) {
		return socket.writePong(data);
	}

	public WebSocketBase textMessageHandler(Handler<String> handler) {
		return socket.textMessageHandler(handler);
	}

	public WebSocketBase binaryMessageHandler(Handler<Buffer> handler) {
		return socket.binaryMessageHandler(handler);
	}

	public WebSocketBase pongHandler(Handler<Buffer> handler) {
		return socket.pongHandler(handler);
	}

	public void end() {
		socket.end();
	}

	public void close() {
		socket.close();
	}

	public void close(short statusCode) {
		socket.close(statusCode);
	}

	public void close(short statusCode, String reason) {
		socket.close(statusCode, reason);
	}

	public SocketAddress remoteAddress() {
		return socket.remoteAddress();
	}

	public SocketAddress localAddress() {
		return socket.localAddress();
	}

	public boolean isSsl() {
		return socket.isSsl();
	}

	public ServerWebSocket fetch(long amount) {
		return socket.fetch(amount);
	}

	@Override
	public void setHandshake(Future<Integer> future) {
		socket.setHandshake(future);
	}
}
