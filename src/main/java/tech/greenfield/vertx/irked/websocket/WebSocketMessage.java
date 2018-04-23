package tech.greenfield.vertx.irked.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import tech.greenfield.vertx.irked.Request;

public class WebSocketMessage implements Buffer {

	private Buffer buffer;
	private Request request;
	private ServerWebSocket socket;
	boolean isBinary;

	/**
	 * Contextual constructor that knows where the message came from
	 * @param request Request that was upgraded to the WebSocket
	 * @param socket WebSocket where the message was received
	 * @param buffer message that was received
	 */
	public WebSocketMessage(Request request, ServerWebSocket socket, Buffer buffer) {
		this.request = request;
		this.socket = socket;
		this.buffer = buffer;
		isBinary = true;
	}

	/**
	 * Cheat constructor for text messages. Vert.x likes to handle text messages in a special way
	 * by just decoding them from UTF-8. For a unified API in exchange for a small performance impact
	 * we convert them back to bytes and let the consumer decide how they want to read them. 
	 * @param request Request that was upgraded to the WebSocket
	 * @param socket WebSocket where the message was received
	 * @param text message that was received
	 */
	public WebSocketMessage(Request request, ServerWebSocket socket, String text) {
		this.request = request;
		this.socket = socket;
		this.buffer = Buffer.buffer(text);
		isBinary = false;
	}

	/**
	 * Retrieve the request that was upgraded to the WebSocket
	 * @return original routing context
	 */
	public Request request() {
		return request;
	}

	/**
	 * Retrieve the WebSocket on which this message was received
	 * @return The socket that handles this message
	 */
	public ServerWebSocket socket() {
		return socket;
	}
	
	/**
	 * Send back a text reply to the other end
	 * @param text Text message to send
	 */
	public void reply(String text) {
		socket.writeTextMessage(text);
	}
	
	/**
	 * Send back a binary reply to the other end
	 * @param buffer Binary data to send
	 */
	public void reply(Buffer buffer) {
		socket.writeBinaryMessage(buffer);
	}
	
	/**
	 * Check whether the incoming message is binary or text
	 * @return {@literal true} if the message is binary
	 */
	public boolean isBinary() {
		return isBinary;
	}

	/**
	 * @param buffer
	 * @see io.vertx.core.shareddata.impl.ClusterSerializable#writeToBuffer(io.vertx.core.buffer.Buffer)
	 */
	public void writeToBuffer(Buffer buffer) {
		buffer.writeToBuffer(buffer);
	}

	/**
	 * @param pos
	 * @param buffer
	 * @return
	 * @see io.vertx.core.shareddata.impl.ClusterSerializable#readFromBuffer(int, io.vertx.core.buffer.Buffer)
	 */
	public int readFromBuffer(int pos, Buffer buffer) {
		return buffer.readFromBuffer(pos, buffer);
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#toString()
	 */
	public String toString() {
		return buffer.toString();
	}

	/**
	 * @param enc
	 * @return
	 * @see io.vertx.core.buffer.Buffer#toString(java.lang.String)
	 */
	public String toString(String enc) {
		return buffer.toString(enc);
	}

	/**
	 * @param enc
	 * @return
	 * @see io.vertx.core.buffer.Buffer#toString(java.nio.charset.Charset)
	 */
	public String toString(Charset enc) {
		return buffer.toString(enc);
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#toJsonObject()
	 */
	public JsonObject toJsonObject() {
		return buffer.toJsonObject();
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#toJsonArray()
	 */
	public JsonArray toJsonArray() {
		return buffer.toJsonArray();
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getByte(int)
	 */
	public byte getByte(int pos) {
		return buffer.getByte(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedByte(int)
	 */
	public short getUnsignedByte(int pos) {
		return buffer.getUnsignedByte(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getInt(int)
	 */
	public int getInt(int pos) {
		return buffer.getInt(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getIntLE(int)
	 */
	public int getIntLE(int pos) {
		return buffer.getIntLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedInt(int)
	 */
	public long getUnsignedInt(int pos) {
		return buffer.getUnsignedInt(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedIntLE(int)
	 */
	public long getUnsignedIntLE(int pos) {
		return buffer.getUnsignedIntLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getLong(int)
	 */
	public long getLong(int pos) {
		return buffer.getLong(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getLongLE(int)
	 */
	public long getLongLE(int pos) {
		return buffer.getLongLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getDouble(int)
	 */
	public double getDouble(int pos) {
		return buffer.getDouble(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getFloat(int)
	 */
	public float getFloat(int pos) {
		return buffer.getFloat(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getShort(int)
	 */
	public short getShort(int pos) {
		return buffer.getShort(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getShortLE(int)
	 */
	public short getShortLE(int pos) {
		return buffer.getShortLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedShort(int)
	 */
	public int getUnsignedShort(int pos) {
		return buffer.getUnsignedShort(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedShortLE(int)
	 */
	public int getUnsignedShortLE(int pos) {
		return buffer.getUnsignedShortLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getMedium(int)
	 */
	public int getMedium(int pos) {
		return buffer.getMedium(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getMediumLE(int)
	 */
	public int getMediumLE(int pos) {
		return buffer.getMediumLE(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedMedium(int)
	 */
	public int getUnsignedMedium(int pos) {
		return buffer.getUnsignedMedium(pos);
	}

	/**
	 * @param pos
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getUnsignedMediumLE(int)
	 */
	public int getUnsignedMediumLE(int pos) {
		return buffer.getUnsignedMediumLE(pos);
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes()
	 */
	public byte[] getBytes() {
		return buffer.getBytes();
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int)
	 */
	public byte[] getBytes(int start, int end) {
		return buffer.getBytes(start, end);
	}

	/**
	 * @param dst
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes(byte[])
	 */
	public Buffer getBytes(byte[] dst) {
		return buffer.getBytes(dst);
	}

	/**
	 * @param dst
	 * @param dstIndex
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes(byte[], int)
	 */
	public Buffer getBytes(byte[] dst, int dstIndex) {
		return buffer.getBytes(dst, dstIndex);
	}

	/**
	 * @param start
	 * @param end
	 * @param dst
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int, byte[])
	 */
	public Buffer getBytes(int start, int end, byte[] dst) {
		return buffer.getBytes(start, end, dst);
	}

	/**
	 * @param start
	 * @param end
	 * @param dst
	 * @param dstIndex
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int, byte[], int)
	 */
	public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
		return buffer.getBytes(start, end, dst, dstIndex);
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getBuffer(int, int)
	 */
	public Buffer getBuffer(int start, int end) {
		return buffer.getBuffer(start, end);
	}

	/**
	 * @param start
	 * @param end
	 * @param enc
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getString(int, int, java.lang.String)
	 */
	public String getString(int start, int end, String enc) {
		return buffer.getString(start, end, enc);
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getString(int, int)
	 */
	public String getString(int start, int end) {
		return buffer.getString(start, end);
	}

	/**
	 * @param buff
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendBuffer(io.vertx.core.buffer.Buffer)
	 */
	public Buffer appendBuffer(Buffer buff) {
		return buffer.appendBuffer(buff);
	}

	/**
	 * @param buff
	 * @param offset
	 * @param len
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendBuffer(io.vertx.core.buffer.Buffer, int, int)
	 */
	public Buffer appendBuffer(Buffer buff, int offset, int len) {
		return buffer.appendBuffer(buff, offset, len);
	}

	/**
	 * @param bytes
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendBytes(byte[])
	 */
	public Buffer appendBytes(byte[] bytes) {
		return buffer.appendBytes(bytes);
	}

	/**
	 * @param bytes
	 * @param offset
	 * @param len
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendBytes(byte[], int, int)
	 */
	public Buffer appendBytes(byte[] bytes, int offset, int len) {
		return buffer.appendBytes(bytes, offset, len);
	}

	/**
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendByte(byte)
	 */
	public Buffer appendByte(byte b) {
		return buffer.appendByte(b);
	}

	/**
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedByte(short)
	 */
	public Buffer appendUnsignedByte(short b) {
		return buffer.appendUnsignedByte(b);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendInt(int)
	 */
	public Buffer appendInt(int i) {
		return buffer.appendInt(i);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendIntLE(int)
	 */
	public Buffer appendIntLE(int i) {
		return buffer.appendIntLE(i);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedInt(long)
	 */
	public Buffer appendUnsignedInt(long i) {
		return buffer.appendUnsignedInt(i);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedIntLE(long)
	 */
	public Buffer appendUnsignedIntLE(long i) {
		return buffer.appendUnsignedIntLE(i);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendMedium(int)
	 */
	public Buffer appendMedium(int i) {
		return buffer.appendMedium(i);
	}

	/**
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendMediumLE(int)
	 */
	public Buffer appendMediumLE(int i) {
		return buffer.appendMediumLE(i);
	}

	/**
	 * @param l
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendLong(long)
	 */
	public Buffer appendLong(long l) {
		return buffer.appendLong(l);
	}

	/**
	 * @param l
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendLongLE(long)
	 */
	public Buffer appendLongLE(long l) {
		return buffer.appendLongLE(l);
	}

	/**
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendShort(short)
	 */
	public Buffer appendShort(short s) {
		return buffer.appendShort(s);
	}

	/**
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendShortLE(short)
	 */
	public Buffer appendShortLE(short s) {
		return buffer.appendShortLE(s);
	}

	/**
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedShort(int)
	 */
	public Buffer appendUnsignedShort(int s) {
		return buffer.appendUnsignedShort(s);
	}

	/**
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedShortLE(int)
	 */
	public Buffer appendUnsignedShortLE(int s) {
		return buffer.appendUnsignedShortLE(s);
	}

	/**
	 * @param f
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendFloat(float)
	 */
	public Buffer appendFloat(float f) {
		return buffer.appendFloat(f);
	}

	/**
	 * @param d
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendDouble(double)
	 */
	public Buffer appendDouble(double d) {
		return buffer.appendDouble(d);
	}

	/**
	 * @param str
	 * @param enc
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendString(java.lang.String, java.lang.String)
	 */
	public Buffer appendString(String str, String enc) {
		return buffer.appendString(str, enc);
	}

	/**
	 * @param str
	 * @return
	 * @see io.vertx.core.buffer.Buffer#appendString(java.lang.String)
	 */
	public Buffer appendString(String str) {
		return buffer.appendString(str);
	}

	/**
	 * @param pos
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setByte(int, byte)
	 */
	public Buffer setByte(int pos, byte b) {
		return buffer.setByte(pos, b);
	}

	/**
	 * @param pos
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setUnsignedByte(int, short)
	 */
	public Buffer setUnsignedByte(int pos, short b) {
		return buffer.setUnsignedByte(pos, b);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setInt(int, int)
	 */
	public Buffer setInt(int pos, int i) {
		return buffer.setInt(pos, i);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setIntLE(int, int)
	 */
	public Buffer setIntLE(int pos, int i) {
		return buffer.setIntLE(pos, i);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setUnsignedInt(int, long)
	 */
	public Buffer setUnsignedInt(int pos, long i) {
		return buffer.setUnsignedInt(pos, i);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setUnsignedIntLE(int, long)
	 */
	public Buffer setUnsignedIntLE(int pos, long i) {
		return buffer.setUnsignedIntLE(pos, i);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setMedium(int, int)
	 */
	public Buffer setMedium(int pos, int i) {
		return buffer.setMedium(pos, i);
	}

	/**
	 * @param pos
	 * @param i
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setMediumLE(int, int)
	 */
	public Buffer setMediumLE(int pos, int i) {
		return buffer.setMediumLE(pos, i);
	}

	/**
	 * @param pos
	 * @param l
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setLong(int, long)
	 */
	public Buffer setLong(int pos, long l) {
		return buffer.setLong(pos, l);
	}

	/**
	 * @param pos
	 * @param l
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setLongLE(int, long)
	 */
	public Buffer setLongLE(int pos, long l) {
		return buffer.setLongLE(pos, l);
	}

	/**
	 * @param pos
	 * @param d
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setDouble(int, double)
	 */
	public Buffer setDouble(int pos, double d) {
		return buffer.setDouble(pos, d);
	}

	/**
	 * @param pos
	 * @param f
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setFloat(int, float)
	 */
	public Buffer setFloat(int pos, float f) {
		return buffer.setFloat(pos, f);
	}

	/**
	 * @param pos
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setShort(int, short)
	 */
	public Buffer setShort(int pos, short s) {
		return buffer.setShort(pos, s);
	}

	/**
	 * @param pos
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setShortLE(int, short)
	 */
	public Buffer setShortLE(int pos, short s) {
		return buffer.setShortLE(pos, s);
	}

	/**
	 * @param pos
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setUnsignedShort(int, int)
	 */
	public Buffer setUnsignedShort(int pos, int s) {
		return buffer.setUnsignedShort(pos, s);
	}

	/**
	 * @param pos
	 * @param s
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setUnsignedShortLE(int, int)
	 */
	public Buffer setUnsignedShortLE(int pos, int s) {
		return buffer.setUnsignedShortLE(pos, s);
	}

	/**
	 * @param pos
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setBuffer(int, io.vertx.core.buffer.Buffer)
	 */
	public Buffer setBuffer(int pos, Buffer b) {
		return buffer.setBuffer(pos, b);
	}

	/**
	 * @param pos
	 * @param b
	 * @param offset
	 * @param len
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setBuffer(int, io.vertx.core.buffer.Buffer, int, int)
	 */
	public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
		return buffer.setBuffer(pos, b, offset, len);
	}

	/**
	 * @param pos
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, java.nio.ByteBuffer)
	 */
	public Buffer setBytes(int pos, ByteBuffer b) {
		return buffer.setBytes(pos, b);
	}

	/**
	 * @param pos
	 * @param b
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, byte[])
	 */
	public Buffer setBytes(int pos, byte[] b) {
		return buffer.setBytes(pos, b);
	}

	/**
	 * @param pos
	 * @param b
	 * @param offset
	 * @param len
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, byte[], int, int)
	 */
	public Buffer setBytes(int pos, byte[] b, int offset, int len) {
		return buffer.setBytes(pos, b, offset, len);
	}

	/**
	 * @param pos
	 * @param str
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setString(int, java.lang.String)
	 */
	public Buffer setString(int pos, String str) {
		return buffer.setString(pos, str);
	}

	/**
	 * @param pos
	 * @param str
	 * @param enc
	 * @return
	 * @see io.vertx.core.buffer.Buffer#setString(int, java.lang.String, java.lang.String)
	 */
	public Buffer setString(int pos, String str, String enc) {
		return buffer.setString(pos, str, enc);
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#length()
	 */
	public int length() {
		return buffer.length();
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#copy()
	 */
	public Buffer copy() {
		return buffer.copy();
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#slice()
	 */
	public Buffer slice() {
		return buffer.slice();
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see io.vertx.core.buffer.Buffer#slice(int, int)
	 */
	public Buffer slice(int start, int end) {
		return buffer.slice(start, end);
	}

	/**
	 * @return
	 * @see io.vertx.core.buffer.Buffer#getByteBuf()
	 */
	public ByteBuf getByteBuf() {
		return buffer.getByteBuf();
	}

}
