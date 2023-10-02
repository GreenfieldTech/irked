package tech.greenfield.vertx.irked.websocket;

import java.security.cert.Certificate;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.net.HostAndPort;
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
	@Override
	public Future<Void> write(Buffer data) {
		return socket.write(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#exceptionHandler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection exceptionHandler(Handler<Throwable> handler) {
		socket.exceptionHandler(handler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#handler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection handler(Handler<Buffer> handler) {
		socket.handler(handler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#pause()
	 */
	@Override
	public WebSocketConnection pause() {
		socket.pause();
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#resume()
	 */
	@Override
	public WebSocketConnection resume() {
		socket.resume();
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#fetch(long)
	 */
	@Override
	public WebSocketConnection fetch(long amount) {
		socket.fetch(amount);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#endHandler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection endHandler(Handler<Void> endHandler) {
		socket.endHandler(endHandler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#setWriteQueueMaxSize(int)
	 */
	@Override
	public WebSocketConnection setWriteQueueMaxSize(int maxSize) {
		socket.setWriteQueueMaxSize(maxSize);
		return this;
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#binaryHandlerID()
	 */
	@Override
	public String binaryHandlerID() {
		return socket.binaryHandlerID();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#drainHandler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection drainHandler(Handler<Void> handler) {
		socket.drainHandler(handler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#closeHandler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection closeHandler(Handler<Void> handler) {
		socket.closeHandler(handler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#textHandlerID()
	 */
	@Override
	public String textHandlerID() {
		return socket.textHandlerID();
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#end(java.lang.Object)
	 */
	@Override
	public Future<Void> end(Buffer data) {
		return socket.end(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#frameHandler(io.vertx.core.Handler)
	 */
	@Override
	public WebSocketConnection frameHandler(Handler<WebSocketFrame> handler) {
		socket.frameHandler(handler);
		return this;
	}

	@Override
	public WebSocketConnection textMessageHandler(Handler<String> handler) {
		socket.textMessageHandler(handler);
		return this;
	}

	@Override
	public WebSocketConnection binaryMessageHandler(Handler<Buffer> handler) {
		socket.binaryMessageHandler(handler);
		return this;
	}

	@Override
	public WebSocketConnection pongHandler(Handler<Buffer> handler) {
		socket.pongHandler(handler);
		return this;
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#uri()
	 */
	@Override
	public String uri() {
		return socket.uri();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#path()
	 */
	@Override
	public String path() {
		return socket.path();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#query()
	 */
	@Override
	public String query() {
		return socket.query();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#accept()
	 */
	@Override
	public void accept() {
		socket.accept();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#subProtocol()
	 */
	@Override
	public String subProtocol() {
		return socket.subProtocol();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#reject()
	 */
	@Override
	public void reject() {
		socket.reject();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#closeStatusCode()
	 */
	@Override
	public Short closeStatusCode() {
		return socket.closeStatusCode();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#closeReason()
	 */
	@Override
	public String closeReason() {
		return socket.closeReason();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#headers()
	 */
	@Override
	public MultiMap headers() {
		return socket.headers();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#reject(int)
	 */
	@Override
	public void reject(int status) {
		socket.reject(status);
	}

	/**
	 * @see io.vertx.core.streams.ReadStream#pipe()
	 */
	@Override
	public Pipe<Buffer> pipe() {
		return socket.pipe();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFrame(io.vertx.core.http.WebSocketFrame)
	 */
	@Override
	public Future<Void> writeFrame(WebSocketFrame frame) {
		return socket.writeFrame(frame);
	}

	/**
	 * @see io.vertx.core.streams.ReadStream#pipeTo(io.vertx.core.streams.WriteStream)
	 */
	@Override
	public Future<Void> pipeTo(WriteStream<Buffer> dst) {
		return socket.pipeTo(dst);
	}

	/**
	 * @see io.vertx.core.streams.WriteStream#writeQueueFull()
	 */
	@Override
	public boolean writeQueueFull() {
		return socket.writeQueueFull();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFinalTextFrame(java.lang.String)
	 */
	@Override
	public Future<Void> writeFinalTextFrame(String text) {
		return socket.writeFinalTextFrame(text);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeFinalBinaryFrame(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Future<Void> writeFinalBinaryFrame(Buffer data) {
		return socket.writeFinalBinaryFrame(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#setHandshake(io.vertx.core.Future)
	 */
	@Override
	public Future<Integer> setHandshake(Future<Integer> future) {
		return socket.setHandshake(future);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeBinaryMessage(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Future<Void> writeBinaryMessage(Buffer data) {
		return socket.writeBinaryMessage(data);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#close()
	 */
	@Override
	public Future<Void> close() {
		return socket.close();
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#sslSession()
	 */
	@Override
	public SSLSession sslSession() {
		return socket.sslSession();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writeTextMessage(java.lang.String)
	 */
	@Override
	public Future<Void> writeTextMessage(String text) {
		return socket.writeTextMessage(text);
	}

	/**
	 * @see io.vertx.core.http.ServerWebSocket#peerCertificateChain()
	 */
	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return socket.peerCertificateChain();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePing(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Future<Void> writePing(Buffer data) {
		return socket.writePing(data);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#writePong(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Future<Void> writePong(Buffer data) {
		return socket.writePong(data);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#end()
	 */
	@Override
	public Future<Void> end() {
		return socket.end();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short)
	 */
	@Override
	public Future<Void> close(short statusCode) {
		return socket.close(statusCode);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#close(short, java.lang.String)
	 */
	@Override
	public Future<Void> close(short statusCode, String reason) {
		return socket.close(statusCode, reason);
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#remoteAddress()
	 */
	@Override
	public SocketAddress remoteAddress() {
		return socket.remoteAddress();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#localAddress()
	 */
	@Override
	public SocketAddress localAddress() {
		return socket.localAddress();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#isSsl()
	 */
	@Override
	public boolean isSsl() {
		return socket.isSsl();
	}

	/**
	 * @see io.vertx.core.http.WebSocketBase#isClosed()
	 */
	@Override
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

	@Override
	public List<Certificate> peerCertificates() throws SSLPeerUnverifiedException {
		return socket.peerCertificates();
	}

	@Override
	public HostAndPort authority() {
		return socket.authority();
	}

}
