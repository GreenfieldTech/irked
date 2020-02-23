package tech.greenfield.vertx.irked.websocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;
import tech.greenfield.vertx.irked.Request;

@SuppressWarnings("deprecation")
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

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.streams.WriteStream#write(java.lang.Object)
	 */
	public Future<Void> write(Buffer data) {
		return socket.write(data);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#exceptionHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket exceptionHandler(Handler<Throwable> handler) {
		return socket.exceptionHandler(handler);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#handler(io.vertx.core.Handler)
	 */
	public ServerWebSocket handler(Handler<Buffer> handler) {
		return socket.handler(handler);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#pause()
	 */
	public ServerWebSocket pause() {
		return socket.pause();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#resume()
	 */
	public ServerWebSocket resume() {
		return socket.resume();
	}

	/**
	 * @param amount
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#fetch(long)
	 */
	public ServerWebSocket fetch(long amount) {
		return socket.fetch(amount);
	}

	/**
	 * @param endHandler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#endHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket endHandler(Handler<Void> endHandler) {
		return socket.endHandler(endHandler);
	}

	/**
	 * @param maxSize
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#setWriteQueueMaxSize(int)
	 */
	public ServerWebSocket setWriteQueueMaxSize(int maxSize) {
		return socket.setWriteQueueMaxSize(maxSize);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#binaryHandlerID()
	 */
	public String binaryHandlerID() {
		return socket.binaryHandlerID();
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#drainHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket drainHandler(Handler<Void> handler) {
		return socket.drainHandler(handler);
	}

	/**
	 * @param data
	 * @param handler
	 * @see io.vertx.core.streams.WriteStream#write(java.lang.Object, io.vertx.core.Handler)
	 */
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		socket.write(data, handler);
	}

	/**
	 * @param frame
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#writeFrame(io.vertx.core.http.WebSocketFrame, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFrame(WebSocketFrame frame, Handler<AsyncResult<Void>> handler) {
		return socket.writeFrame(frame, handler);
	}

	/**
	 * @param text
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#writeFinalTextFrame(java.lang.String, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFinalTextFrame(String text, Handler<AsyncResult<Void>> handler) {
		return socket.writeFinalTextFrame(text, handler);
	}

	/**
	 * @param data
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#writeFinalBinaryFrame(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFinalBinaryFrame(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writeFinalBinaryFrame(data, handler);
	}

	/**
	 * @param data
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#writeBinaryMessage(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeBinaryMessage(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writeBinaryMessage(data, handler);
	}

	/**
	 * @param text
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#writeTextMessage(java.lang.String, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeTextMessage(String text, Handler<AsyncResult<Void>> handler) {
		return socket.writeTextMessage(text, handler);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#closeHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket closeHandler(Handler<Void> handler) {
		return socket.closeHandler(handler);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#textHandlerID()
	 */
	public String textHandlerID() {
		return socket.textHandlerID();
	}

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.streams.WriteStream#end(java.lang.Object)
	 */
	public Future<Void> end(Buffer data) {
		return socket.end(data);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#frameHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket frameHandler(Handler<WebSocketFrame> handler) {
		return socket.frameHandler(handler);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#uri()
	 */
	public String uri() {
		return socket.uri();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#path()
	 */
	public String path() {
		return socket.path();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#query()
	 */
	public String query() {
		return socket.query();
	}

	/**
	 *
	 * @see io.vertx.core.http.ServerWebSocket#accept()
	 */
	public void accept() {
		socket.accept();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#subProtocol()
	 */
	public String subProtocol() {
		return socket.subProtocol();
	}

	/**
	 * @param data
	 * @param handler
	 * @see io.vertx.core.streams.WriteStream#end(java.lang.Object, io.vertx.core.Handler)
	 */
	public void end(Buffer data, Handler<AsyncResult<Void>> handler) {
		socket.end(data, handler);
	}

	/**
	 *
	 * @see io.vertx.core.http.ServerWebSocket#reject()
	 */
	public void reject() {
		socket.reject();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#closeStatusCode()
	 */
	public Short closeStatusCode() {
		return socket.closeStatusCode();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#closeReason()
	 */
	public String closeReason() {
		return socket.closeReason();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#headers()
	 */
	public MultiMap headers() {
		return socket.headers();
	}

	/**
	 * @param status
	 * @see io.vertx.core.http.ServerWebSocket#reject(int)
	 */
	public void reject(int status) {
		socket.reject(status);
	}

	/**
	 * @return
	 * @see io.vertx.core.streams.ReadStream#pipe()
	 */
	public Pipe<Buffer> pipe() {
		return socket.pipe();
	}

	/**
	 * @param future
	 * @param handler
	 * @see io.vertx.core.http.ServerWebSocket#setHandshake(io.vertx.core.Future, io.vertx.core.Handler)
	 */
	public void setHandshake(Future<Integer> future, Handler<AsyncResult<Integer>> handler) {
		socket.setHandshake(future, handler);
	}

	/**
	 * @param frame
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writeFrame(io.vertx.core.http.WebSocketFrame)
	 */
	public Future<Void> writeFrame(WebSocketFrame frame) {
		return socket.writeFrame(frame);
	}

	/**
	 * @param dst
	 * @return
	 * @see io.vertx.core.streams.ReadStream#pipeTo(io.vertx.core.streams.WriteStream)
	 */
	public Future<Void> pipeTo(WriteStream<Buffer> dst) {
		return socket.pipeTo(dst);
	}

	/**
	 * @return
	 * @see io.vertx.core.streams.WriteStream#writeQueueFull()
	 */
	public boolean writeQueueFull() {
		return socket.writeQueueFull();
	}

	/**
	 * @param text
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writeFinalTextFrame(java.lang.String)
	 */
	public Future<Void> writeFinalTextFrame(String text) {
		return socket.writeFinalTextFrame(text);
	}

	/**
	 * @param dst
	 * @param handler
	 * @see io.vertx.core.streams.ReadStream#pipeTo(io.vertx.core.streams.WriteStream, io.vertx.core.Handler)
	 */
	public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
		socket.pipeTo(dst, handler);
	}

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writeFinalBinaryFrame(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writeFinalBinaryFrame(Buffer data) {
		return socket.writeFinalBinaryFrame(data);
	}

	/**
	 * @param future
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#setHandshake(io.vertx.core.Future)
	 */
	public Future<Integer> setHandshake(Future<Integer> future) {
		return socket.setHandshake(future);
	}

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writeBinaryMessage(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writeBinaryMessage(Buffer data) {
		return socket.writeBinaryMessage(data);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#close()
	 */
	public Future<Void> close() {
		return socket.close();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.ServerWebSocket#sslSession()
	 */
	public SSLSession sslSession() {
		return socket.sslSession();
	}

	/**
	 * @param text
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writeTextMessage(java.lang.String)
	 */
	public Future<Void> writeTextMessage(String text) {
		return socket.writeTextMessage(text);
	}

	/**
	 * @return
	 * @throws SSLPeerUnverifiedException
	 * @see io.vertx.core.http.ServerWebSocket#peerCertificateChain()
	 */
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return socket.peerCertificateChain();
	}

	/**
	 * @param data
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writePing(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public WebSocketBase writePing(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writePing(data, handler);
	}

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writePing(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writePing(Buffer data) {
		return socket.writePing(data);
	}

	/**
	 * @param data
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writePong(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public WebSocketBase writePong(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writePong(data, handler);
	}

	/**
	 * @param data
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#writePong(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writePong(Buffer data) {
		return socket.writePong(data);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#textMessageHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase textMessageHandler(Handler<String> handler) {
		return socket.textMessageHandler(handler);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#binaryMessageHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase binaryMessageHandler(Handler<Buffer> handler) {
		return socket.binaryMessageHandler(handler);
	}

	/**
	 * @param handler
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#pongHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase pongHandler(Handler<Buffer> handler) {
		return socket.pongHandler(handler);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#end()
	 */
	public Future<Void> end() {
		return socket.end();
	}

	/**
	 * @param handler
	 * @see io.vertx.core.http.WebSocketBase#end(io.vertx.core.Handler)
	 */
	public void end(Handler<AsyncResult<Void>> handler) {
		socket.end(handler);
	}

	/**
	 * @param handler
	 * @see io.vertx.core.http.WebSocketBase#close(io.vertx.core.Handler)
	 */
	public void close(Handler<AsyncResult<Void>> handler) {
		socket.close(handler);
	}

	/**
	 * @param statusCode
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#close(short)
	 */
	public Future<Void> close(short statusCode) {
		return socket.close(statusCode);
	}

	/**
	 * @param statusCode
	 * @param handler
	 * @see io.vertx.core.http.WebSocketBase#close(short, io.vertx.core.Handler)
	 */
	public void close(short statusCode, Handler<AsyncResult<Void>> handler) {
		socket.close(statusCode, handler);
	}

	/**
	 * @param statusCode
	 * @param reason
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#close(short, java.lang.String)
	 */
	public Future<Void> close(short statusCode, String reason) {
		return socket.close(statusCode, reason);
	}

	/**
	 * @param statusCode
	 * @param reason
	 * @param handler
	 * @see io.vertx.core.http.WebSocketBase#close(short, java.lang.String, io.vertx.core.Handler)
	 */
	public void close(short statusCode, String reason, Handler<AsyncResult<Void>> handler) {
		socket.close(statusCode, reason, handler);
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#remoteAddress()
	 */
	public SocketAddress remoteAddress() {
		return socket.remoteAddress();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#localAddress()
	 */
	public SocketAddress localAddress() {
		return socket.localAddress();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#isSsl()
	 */
	public boolean isSsl() {
		return socket.isSsl();
	}

	/**
	 * @return
	 * @see io.vertx.core.http.WebSocketBase#isClosed()
	 */
	public boolean isClosed() {
		return socket.isClosed();
	}

}
