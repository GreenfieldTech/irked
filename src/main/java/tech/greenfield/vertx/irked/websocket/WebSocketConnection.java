package tech.greenfield.vertx.irked.websocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static Logger log = LoggerFactory.getLogger(WebSocketConnection.class);

	public WebSocketConnection(Request request, Handler<? super WebSocketMessage> handler) {
		(this.request = request).request().toWebSocket().onSuccess(s -> {
			socket = s;
			socket.binaryMessageHandler(buffer -> handler.handle(new WebSocketMessage(request, socket, buffer)));
			// The text message handler is more expensive because it effectively causes UTF-8 decoding + encoding + (probably) final decoding
			// we should implement a custom frame aggregator that allows us to delay decoding text until the user actually wants to.
			socket.textMessageHandler(text -> handler.handle(new WebSocketMessage(request, socket, text)));
		}).onFailure(t -> {
			log.error("Failed to upgrade websocket",t);
		});
	}

	/**
	 * Retrieve the Irked {@link Request} object that originated this WebSocket connection
	 * @return Irked HTTP request routing context wrapper
	 */
	public Request request() {
		return request;
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#write(java.lang.Object)
	 */
	public Future<Void> write(Buffer data) {
		return socket.write(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#exceptionHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket exceptionHandler(Handler<Throwable> handler) {
		return socket.exceptionHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#handler(io.vertx.core.Handler)
	 */
	public ServerWebSocket handler(Handler<Buffer> handler) {
		return socket.handler(handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#pause()
	 */
	public ServerWebSocket pause() {
		return socket.pause();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#resume()
	 */
	public ServerWebSocket resume() {
		return socket.resume();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#fetch(long)
	 */
	public ServerWebSocket fetch(long amount) {
		return socket.fetch(amount);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#endHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket endHandler(Handler<Void> endHandler) {
		return socket.endHandler(endHandler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#setWriteQueueMaxSize(int)
	 */
	public ServerWebSocket setWriteQueueMaxSize(int maxSize) {
		return socket.setWriteQueueMaxSize(maxSize);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#binaryHandlerID()
	 */
	public String binaryHandlerID() {
		return socket.binaryHandlerID();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#drainHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket drainHandler(Handler<Void> handler) {
		return socket.drainHandler(handler);
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#write(java.lang.Object, io.vertx.core.Handler)
	 */
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		socket.write(data, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#writeFrame(io.vertx.core.http.WebSocketFrame, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFrame(WebSocketFrame frame, Handler<AsyncResult<Void>> handler) {
		return socket.writeFrame(frame, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#writeFinalTextFrame(java.lang.String, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFinalTextFrame(String text, Handler<AsyncResult<Void>> handler) {
		return socket.writeFinalTextFrame(text, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#writeFinalBinaryFrame(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeFinalBinaryFrame(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writeFinalBinaryFrame(data, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#writeBinaryMessage(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeBinaryMessage(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writeBinaryMessage(data, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#writeTextMessage(java.lang.String, io.vertx.core.Handler)
	 */
	public ServerWebSocket writeTextMessage(String text, Handler<AsyncResult<Void>> handler) {
		return socket.writeTextMessage(text, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#closeHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket closeHandler(Handler<Void> handler) {
		return socket.closeHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#textHandlerID()
	 */
	public String textHandlerID() {
		return socket.textHandlerID();
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#end(java.lang.Object)
	 */
	public Future<Void> end(Buffer data) {
		return socket.end(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#frameHandler(io.vertx.core.Handler)
	 */
	public ServerWebSocket frameHandler(Handler<WebSocketFrame> handler) {
		return socket.frameHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#uri()
	 */
	public String uri() {
		return socket.uri();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#path()
	 */
	public String path() {
		return socket.path();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#query()
	 */
	public String query() {
		return socket.query();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#accept()
	 */
	public void accept() {
		socket.accept();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#subProtocol()
	 */
	public String subProtocol() {
		return socket.subProtocol();
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#end(java.lang.Object, io.vertx.core.Handler)
	 */
	public void end(Buffer data, Handler<AsyncResult<Void>> handler) {
		socket.end(data, handler);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#reject()
	 */
	public void reject() {
		socket.reject();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#closeStatusCode()
	 */
	public Short closeStatusCode() {
		return socket.closeStatusCode();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#closeReason()
	 */
	public String closeReason() {
		return socket.closeReason();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#headers()
	 */
	public MultiMap headers() {
		return socket.headers();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#reject(int)
	 */
	public void reject(int status) {
		socket.reject(status);
	}

	/**
	 * @see io.vertx.core.streams.ReadStream#pipe()
	 */
	public Pipe<Buffer> pipe() {
		return socket.pipe();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#setHandshake(io.vertx.core.Future, io.vertx.core.Handler)
	 */
	public void setHandshake(Future<Integer> future, Handler<AsyncResult<Integer>> handler) {
		socket.setHandshake(future, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFrame(io.vertx.core.http.WebSocketFrame)
	 */
	public Future<Void> writeFrame(WebSocketFrame frame) {
		return socket.writeFrame(frame);
	}

	/**
	 * @see io.vertx.core.streams.ReadStream#pipeTo(io.vertx.core.streams.WriteStream)
	 */
	public Future<Void> pipeTo(WriteStream<Buffer> dst) {
		return socket.pipeTo(dst);
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#writeQueueFull()
	 */
	public boolean writeQueueFull() {
		return socket.writeQueueFull();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFinalTextFrame(java.lang.String)
	 */
	public Future<Void> writeFinalTextFrame(String text) {
		return socket.writeFinalTextFrame(text);
	}

	/**
	 * @see io.vertx.core.streams.ReadStream#pipeTo(io.vertx.core.streams.WriteStream, io.vertx.core.Handler)
	 */
	public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
		socket.pipeTo(dst, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFinalBinaryFrame(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writeFinalBinaryFrame(Buffer data) {
		return socket.writeFinalBinaryFrame(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#setHandshake(io.vertx.core.Future)
	 */
	public Future<Integer> setHandshake(Future<Integer> future) {
		return socket.setHandshake(future);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeBinaryMessage(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writeBinaryMessage(Buffer data) {
		return socket.writeBinaryMessage(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#close()
	 */
	public Future<Void> close() {
		return socket.close();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#sslSession()
	 */
	public SSLSession sslSession() {
		return socket.sslSession();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeTextMessage(java.lang.String)
	 */
	public Future<Void> writeTextMessage(String text) {
		return socket.writeTextMessage(text);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#peerCertificateChain()
	 */
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return socket.peerCertificateChain();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePing(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public WebSocketBase writePing(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writePing(data, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePing(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writePing(Buffer data) {
		return socket.writePing(data);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePong(io.vertx.core.buffer.Buffer, io.vertx.core.Handler)
	 */
	public WebSocketBase writePong(Buffer data, Handler<AsyncResult<Void>> handler) {
		return socket.writePong(data, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePong(io.vertx.core.buffer.Buffer)
	 */
	public Future<Void> writePong(Buffer data) {
		return socket.writePong(data);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#textMessageHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase textMessageHandler(Handler<String> handler) {
		return socket.textMessageHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#binaryMessageHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase binaryMessageHandler(Handler<Buffer> handler) {
		return socket.binaryMessageHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#pongHandler(io.vertx.core.Handler)
	 */
	public WebSocketBase pongHandler(Handler<Buffer> handler) {
		return socket.pongHandler(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#end()
	 */
	public Future<Void> end() {
		return socket.end();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#end(io.vertx.core.Handler)
	 */
	public void end(Handler<AsyncResult<Void>> handler) {
		socket.end(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(io.vertx.core.Handler)
	 */
	public void close(Handler<AsyncResult<Void>> handler) {
		socket.close(handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short)
	 */
	public Future<Void> close(short statusCode) {
		return socket.close(statusCode);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short, io.vertx.core.Handler)
	 */
	public void close(short statusCode, Handler<AsyncResult<Void>> handler) {
		socket.close(statusCode, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short, java.lang.String)
	 */
	public Future<Void> close(short statusCode, String reason) {
		return socket.close(statusCode, reason);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short, java.lang.String, io.vertx.core.Handler)
	 */
	public void close(short statusCode, String reason, Handler<AsyncResult<Void>> handler) {
		socket.close(statusCode, reason, handler);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#remoteAddress()
	 */
	public SocketAddress remoteAddress() {
		return socket.remoteAddress();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#localAddress()
	 */
	public SocketAddress localAddress() {
		return socket.localAddress();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#isSsl()
	 */
	public boolean isSsl() {
		return socket.isSsl();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#isClosed()
	 */
	public boolean isClosed() {
		return socket.isClosed();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#scheme()
	 */
	@Override
	public String scheme() {
		return socket.scheme();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#host()
	 */
	@Override
	public String host() {
		return socket.host();
	}

}
