package tech.greenfield.vertx.irked.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

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
	public WebSocketMessage reply(String text) {
		socket.writeTextMessage(text);
		return this;
	}
	
	/**
	 * Send back a binary reply to the other end
	 * @param buffer Binary data to send
	 */
	public WebSocketMessage reply(Buffer buffer) {
		socket.writeBinaryMessage(buffer);
		return this;
	}
	
	/**
	 * Check whether the incoming message is binary or text
	 * @return {@literal true} if the message is binary
	 */
	public boolean isBinary() {
		return isBinary;
	}

	/**
	 * @param buffer Buffer to marshal into
	 * @see io.vertx.core.shareddata.impl.ClusterSerializable#writeToBuffer(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public void writeToBuffer(Buffer buffer) {
		buffer.writeToBuffer(buffer);
	}

	/**
	 * @param pos Position in the buffer to start unmarshaling from
	 * @param buffer Buffer to unmarshal from
	 * @return amount of bytes read from the buffer
	 * @see io.vertx.core.shareddata.impl.ClusterSerializable#readFromBuffer(int, io.vertx.core.buffer.Buffer)
	 */
	@Override
	public int readFromBuffer(int pos, Buffer buffer) {
		return buffer.readFromBuffer(pos, buffer);
	}

	/**
	 * @return a {@code String} representation of the Buffer with the {@code UTF-8 }encoding
	 * @see io.vertx.core.buffer.Buffer#toString()
	 */
	@Override
	public String toString() {
		return buffer.toString();
	}

	/**
	 * @param enc String encoding to use
	 * @return a {@code String} representation of the Buffer with the encoding specified by {@code enc}
	 * @see io.vertx.core.buffer.Buffer#toString(java.lang.String)
	 */
	@Override
	public String toString(String enc) {
		return buffer.toString(enc);
	}

	/**
	 * @param enc String encoding to use
	 * @return a {@code String} representation of the Buffer with the encoding specified by {@code enc}
	 * @see io.vertx.core.buffer.Buffer#toString(java.nio.charset.Charset)
	 */
	@Override
	public String toString(Charset enc) {
		return buffer.toString(enc);
	}

	/**
	 * @return a {@code String} representation of the Buffer with the encoding specified by {@code enc}
	 * @see io.vertx.core.buffer.Buffer#toJsonObject()
	 */
	@Override
	public JsonObject toJsonObject() {
		return buffer.toJsonObject();
	}

	/**
	 * @return a Json array representation of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#toJsonArray()
	 */
	@Override
	public JsonArray toJsonArray() {
		return buffer.toJsonArray();
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the {@code byte} at position {@code pos} in the Buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 1} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getByte(int)
	 */
	@Override
	public byte getByte(int pos) {
		return buffer.getByte(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the unsigned {@code byte} at position {@code pos} in the Buffer, as a {@code short}.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 1} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedByte(int)
	 */
	@Override
	public short getUnsignedByte(int pos) {
		return buffer.getUnsignedByte(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the {@code int} at position {@code pos} in the Buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 4} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getInt(int)
	 */
	@Override
	public int getInt(int pos) {
		return buffer.getInt(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return a 32-bit integer at the specified absolute {@code index} in this buffer with Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 4} is greater than {@code this.capacity}
	 * @see io.vertx.core.buffer.Buffer#getIntLE(int)
	 */
	@Override
	public int getIntLE(int pos) {
		return buffer.getIntLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the unsigned {@code int} at position {@code pos} in the Buffer, as a {@code long}.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 4} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedInt(int)
	 */
	@Override
	public long getUnsignedInt(int pos) {
		return buffer.getUnsignedInt(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the unsigned {@code int} at position {@code pos} in the Buffer, as a {@code long} in Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 4} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedIntLE(int)
	 */
	@Override
	public long getUnsignedIntLE(int pos) {
		return buffer.getUnsignedIntLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the {@code long} at position {@code pos} in the Buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 8} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getLong(int)
	 */
	@Override
	public long getLong(int pos) {
		return buffer.getLong(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return a 64-bit long integer at the specified absolute {@code index} in this buffer in Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 8} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getLongLE(int)
	 */
	@Override
	public long getLongLE(int pos) {
		return buffer.getLongLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
  	 * @return the {@code double} at position {@code pos} in the Buffer.
  	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 8} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getDouble(int)
	 */
	@Override
	public double getDouble(int pos) {
		return buffer.getDouble(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the {@code float} at position {@code pos} in the Buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 4} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getFloat(int)
	 */
	@Override
	public float getFloat(int pos) {
		return buffer.getFloat(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the {@code short} at position {@code pos} in the Buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 2} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getShort(int)
	 */
	@Override
	public short getShort(int pos) {
		return buffer.getShort(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return a 16-bit short integer at the specified absolute {@code index} in this buffer in Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 2} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getShortLE(int)
	 */
	@Override
	public short getShortLE(int pos) {
		return buffer.getShortLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return the unsigned {@code short} at position {@code pos} in the Buffer, as an {@code int}.
	 * @throws IndexOutOfBoundsException if the specified {@code pos} is less than {@code 0} or {@code pos + 2} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedShort(int)
	 */
	@Override
	public int getUnsignedShort(int pos) {
		return buffer.getUnsignedShort(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return an unsigned 16-bit short integer at the specified absolute {@code index} in this buffer in Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 2} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedShortLE(int)
	 */
	@Override
	public int getUnsignedShortLE(int pos) {
		return buffer.getUnsignedShortLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return a 24-bit medium integer at the specified absolute {@code index} in this buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 3} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getMedium(int)
	 */
	@Override
	public int getMedium(int pos) {
		return buffer.getMedium(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return a 24-bit medium integer at the specified absolute {@code index} in this buffer in the Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 3} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getMediumLE(int)
	 */
	@Override
	public int getMediumLE(int pos) {
		return buffer.getMediumLE(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return an unsigned 24-bit medium integer at the specified absolute {@code index} in this buffer.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 3} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedMedium(int)
	 */
	@Override
	public int getUnsignedMedium(int pos) {
		return buffer.getUnsignedMedium(pos);
	}

	/**
	 * @param pos Position in the buffer to check
	 * @return an unsigned 24-bit medium integer at the specified absolute {@code index} in this buffer in Little Endian Byte Order.
	 * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or {@code index + 3} is greater than the length of the Buffer.
	 * @see io.vertx.core.buffer.Buffer#getUnsignedMediumLE(int)
	 */
	@Override
	public int getUnsignedMediumLE(int pos) {
		return buffer.getUnsignedMediumLE(pos);
	}

	/**
	 * @return a copy of the entire Buffer as a {@code byte[]}
	 * @see io.vertx.core.buffer.Buffer#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return buffer.getBytes();
	}

	/**
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @return a copy of a sub-sequence the Buffer as a {@code byte[]} starting at position {@code start}
	 * 	and ending at position {@code end - 1}
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int)
	 */
	@Override
	public byte[] getBytes(int start, int end) {
		return buffer.getBytes(start, end);
	}

	/**
	 * Transfers the content of the Buffer into a {@code byte[]}.
	 *
	 * @param dst the destination byte array
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @throws IndexOutOfBoundsException if the content of the Buffer cannot fit into the destination byte array
	 * @see io.vertx.core.buffer.Buffer#getBytes(byte[])
	 */
	@Override
	public Buffer getBytes(byte[] dst) {
		buffer.getBytes(dst);
		return this;
	}

	/**
	 * Transfers the content of the Buffer into a {@code byte[]} at the specific destination.
	 *
	 * @param dst the destination byte array
	 * @param dstIndex the index into the destination array where to star the copy
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @throws IndexOutOfBoundsException if the content of the Buffer cannot fit into the destination byte array
	 * @see io.vertx.core.buffer.Buffer#getBytes(byte[], int)
	 */
	@Override
	public Buffer getBytes(byte[] dst, int dstIndex) {
		buffer.getBytes(dst, dstIndex);
		return this;
	}

	/**
	 * Transfers the content of the Buffer starting at position {@code start} and ending at position {@code end - 1}
	 * into a {@code byte[]}.
	 *
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @param dst the destination byte array
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @throws IndexOutOfBoundsException if the content of the Buffer cannot fit into the destination byte array
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int, byte[])
	 */
	@Override
	public Buffer getBytes(int start, int end, byte[] dst) {
		buffer.getBytes(start, end, dst);
		return this;
	}

	/**
	 * Transfers the content of the Buffer starting at position {@code start} and ending at position {@code end - 1}
	 * into a {@code byte[]} at the specific destination.
	 *
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @param dst the destination byte array
	 * @param dstIndex the index into the destination array where to star the copy
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @throws IndexOutOfBoundsException if the content of the Buffer cannot fit into the destination byte array
	 * @see io.vertx.core.buffer.Buffer#getBytes(int, int, byte[], int)
	 */
	@Override
	public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
		buffer.getBytes(start, end, dst, dstIndex);
		return this;
	}

	/**
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @return a copy of a sub-sequence the Buffer as a {@link io.vertx.core.buffer.Buffer} starting at position {@code start}
	 * and ending at position {@code end - 1}
	 * @see io.vertx.core.buffer.Buffer#getBuffer(int, int)
	 */
	@Override
	public Buffer getBuffer(int start, int end) {
		return buffer.getBuffer(start, end);
	}

	/**
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @param enc String encoding to use
	 * @return a copy of a sub-sequence the Buffer as a {@code String} starting at position {@code start}
	 * and ending at position {@code end - 1} interpreted as a String in the specified encoding
	 * @see io.vertx.core.buffer.Buffer#getString(int, int, java.lang.String)
	 */
	@Override
	public String getString(int start, int end, String enc) {
		return buffer.getString(start, end, enc);
	}

	/**
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @return a copy of a sub-sequence the Buffer as a {@code String} starting at position {@code start}
	 * and ending at position {@code end - 1} interpreted as a String in UTF-8 encoding
	 * @see io.vertx.core.buffer.Buffer#getString(int, int)
	 */
	@Override
	public String getString(int start, int end) {
		return buffer.getString(start, end);
	}

	/**
	 * Appends the specified {@code Buffer} to the end of this Buffer. The buffer will expand as necessary to accommodate
	 * any bytes written.<p>
	 * @param buff buffer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendBuffer(io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Buffer appendBuffer(Buffer buff) {
		buffer.appendBuffer(buff);
		return this;
	}

	/**
	 * Appends the specified {@code Buffer} starting at the {@code offset} using {@code len} to the end of this Buffer. The buffer will expand as necessary to accommodate
	 * any bytes written.<p>
	 * @param buff buffer to append
	 * @param offset offset where to start appending 
	 * @param len length of the buffer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendBuffer(io.vertx.core.buffer.Buffer, int, int)
	 */
	@Override
	public Buffer appendBuffer(Buffer buff, int offset, int len) {
		buffer.appendBuffer(buff, offset, len);
		return this;
	}

	/**
	 * Appends the specified {@code byte[]} to the end of the Buffer. The buffer will expand as necessary to accommodate any bytes written.<p>
	 * @param bytes bytes to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendBytes(byte[])
	 */
	@Override
	public Buffer appendBytes(byte[] bytes) {
		buffer.appendBytes(bytes);
		return this;
	}

	/**
	 * Appends the specified number of bytes from {@code byte[]} to the end of the Buffer, starting at the given offset.
	 * The buffer will expand as necessary to accommodate any bytes written.<p>
	 * @param bytes bytes to append
	 * @param offset offset where to start appending 
	 * @param len length of the buffer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendBytes(byte[], int, int)
	 */
	@Override
	public Buffer appendBytes(byte[] bytes, int offset, int len) {
		buffer.appendBytes(bytes, offset, len);
		return this;
	}

	/**
	 * Appends the specified {@code byte} to the end of the Buffer. The buffer will expand as necessary to accommodate any bytes written.<p>
	 * @param b byte to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendByte(byte)
	 */
	@Override
	public Buffer appendByte(byte b) {
		buffer.appendByte(b);
		return this;
	}

	/**
	 * Appends the specified {@code byte} to the end of the Buffer. The buffer will expand as necessary to accommodate any bytes written.<p>
	 * @param b byte to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedByte(short)
	 */
	@Override
	public Buffer appendUnsignedByte(short b) {
		buffer.appendUnsignedByte(b);
		return this;
	}

	/**
	 * Appends the specified {@code int} to the end of the Buffer. The buffer will expand as necessary to accommodate any bytes written.<p>
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendInt(int)
	 */
	@Override
	public Buffer appendInt(int i) {
		buffer.appendInt(i);
		return this;
	}

	/**
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendIntLE(int)
	 */
	@Override
	public Buffer appendIntLE(int i) {
		buffer.appendIntLE(i);
		return this;
	}

	/**
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedInt(long)
	 */
	@Override
	public Buffer appendUnsignedInt(long i) {
		buffer.appendUnsignedInt(i);
		return this;
	}

	/**
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedIntLE(long)
	 */
	@Override
	public Buffer appendUnsignedIntLE(long i) {
		buffer.appendUnsignedIntLE(i);
		return this;
	}

	/**
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendMedium(int)
	 */
	@Override
	public Buffer appendMedium(int i) {
		buffer.appendMedium(i);
		return this;
	}

	/**
	 * @param i integer to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendMediumLE(int)
	 */
	@Override
	public Buffer appendMediumLE(int i) {
		buffer.appendMediumLE(i);
		return this;
	}

	/**
	 * @param l long to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendLong(long)
	 */
	@Override
	public Buffer appendLong(long l) {
		buffer.appendLong(l);
		return this;
	}

	/**
	 * @param l long to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendLongLE(long)
	 */
	@Override
	public Buffer appendLongLE(long l) {
		buffer.appendLongLE(l);
		return this;
	}

	/**
	 * @param s short to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendShort(short)
	 */
	@Override
	public Buffer appendShort(short s) {
		buffer.appendShort(s);
		return this;
	}

	/**
	 * @param s short to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendShortLE(short)
	 */
	@Override
	public Buffer appendShortLE(short s) {
		buffer.appendShortLE(s);
		return this;
	}

	/**
	 * @param s short to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedShort(int)
	 */
	@Override
	public Buffer appendUnsignedShort(int s) {
		buffer.appendUnsignedShort(s);
		return this;
	}

	/**
	 * @param s short to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendUnsignedShortLE(int)
	 */
	@Override
	public Buffer appendUnsignedShortLE(int s) {
		buffer.appendUnsignedShortLE(s);
		return this;
	}

	/**
	 * @param f float to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendFloat(float)
	 */
	@Override
	public Buffer appendFloat(float f) {
		buffer.appendFloat(f);
		return this;
	}

	/**
	 * @param d double to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendDouble(double)
	 */
	@Override
	public Buffer appendDouble(double d) {
		buffer.appendDouble(d);
		return this;
	}

	/**
	 * @param str String to append
	 * @param enc encoding to use for the string
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendString(java.lang.String, java.lang.String)
	 */
	@Override
	public Buffer appendString(String str, String enc) {
		buffer.appendString(str, enc);
		return this;
	}

	/**
	 * @param str string to append
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#appendString(java.lang.String)
	 */
	@Override
	public Buffer appendString(String str) {
		buffer.appendString(str);
		return this;
	}

	/**
	 * @param pos position where to write the byte
	 * @param b byte to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setByte(int, byte)
	 */
	@Override
	public Buffer setByte(int pos, byte b) {
		buffer.setByte(pos, b);
		return this;
	}

	/**
	 * @param pos position where to write the short
	 * @param b short to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setUnsignedByte(int, short)
	 */
	@Override
	public Buffer setUnsignedByte(int pos, short b) {
		buffer.setUnsignedByte(pos, b);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setInt(int, int)
	 */
	@Override
	public Buffer setInt(int pos, int i) {
		buffer.setInt(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setIntLE(int, int)
	 */
	@Override
	public Buffer setIntLE(int pos, int i) {
		buffer.setIntLE(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setUnsignedInt(int, long)
	 */
	@Override
	public Buffer setUnsignedInt(int pos, long i) {
		buffer.setUnsignedInt(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setUnsignedIntLE(int, long)
	 */
	@Override
	public Buffer setUnsignedIntLE(int pos, long i) {
		buffer.setUnsignedIntLE(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setMedium(int, int)
	 */
	@Override
	public Buffer setMedium(int pos, int i) {
		buffer.setMedium(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the integer
	 * @param i integer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setMediumLE(int, int)
	 */
	@Override
	public Buffer setMediumLE(int pos, int i) {
		buffer.setMediumLE(pos, i);
		return this;
	}

	/**
	 * @param pos position where to write the long
	 * @param l long to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setLong(int, long)
	 */
	@Override
	public Buffer setLong(int pos, long l) {
		buffer.setLong(pos, l);
		return this;
	}

	/**
	 * @param pos position where to write the long
	 * @param l long to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setLongLE(int, long)
	 */
	@Override
	public Buffer setLongLE(int pos, long l) {
		buffer.setLongLE(pos, l);
		return this;
	}

	/**
	 * @param pos position where to write the double
	 * @param d double to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setDouble(int, double)
	 */
	@Override
	public Buffer setDouble(int pos, double d) {
		buffer.setDouble(pos, d);
		return this;
	}

	/**
	 * @param pos position where to write the float
	 * @param f float to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setFloat(int, float)
	 */
	@Override
	public Buffer setFloat(int pos, float f) {
		buffer.setFloat(pos, f);
		return this;
	}

	/**
	 * @param pos position where to write the short
	 * @param s short to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setShort(int, short)
	 */
	@Override
	public Buffer setShort(int pos, short s) {
		buffer.setShort(pos, s);
		return this;
	}

	/**
	 * @param pos position where to write the short
	 * @param s short to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setShortLE(int, short)
	 */
	@Override
	public Buffer setShortLE(int pos, short s) {
		buffer.setShortLE(pos, s);
		return this;
	}

	/**
	 * @param pos position where to write the short
	 * @param s short to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setUnsignedShort(int, int)
	 */
	@Override
	public Buffer setUnsignedShort(int pos, int s) {
		buffer.setUnsignedShort(pos, s);
		return this;
	}

	/**
	 * @param pos position where to write the short
	 * @param s short to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setUnsignedShortLE(int, int)
	 */
	@Override
	public Buffer setUnsignedShortLE(int pos, int s) {
		buffer.setUnsignedShortLE(pos, s);
		return this;
	}

	/**
	 * @param pos position where to write the buffer
	 * @param b buffer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setBuffer(int, io.vertx.core.buffer.Buffer)
	 */
	@Override
	public Buffer setBuffer(int pos, Buffer b) {
		buffer.setBuffer(pos, b);
		return this;
	}

	/**
	 * @param pos position where to write the buffer
	 * @param b buffer to write
	 * @param offset offset into the source buffer to read from
	 * @param len amount of bytes to read from the source buffer
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setBuffer(int, io.vertx.core.buffer.Buffer, int, int)
	 */
	@Override
	public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
		buffer.setBuffer(pos, b, offset, len);
		return this;
	}

	/**
	 * @param pos position where to write the buffer
	 * @param b buffer to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, java.nio.ByteBuffer)
	 */
	@Override
	public Buffer setBytes(int pos, ByteBuffer b) {
		buffer.setBytes(pos, b);
		return this;
	}

	/**
	 * @param pos position where to write the bytes
	 * @param b bytes to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, byte[])
	 */
	@Override
	public Buffer setBytes(int pos, byte[] b) {
		buffer.setBytes(pos, b);
		return this;
	}

	/**
	 * @param pos position where to write the bytes
	 * @param b bytes to write
	 * @param offset offset into the source byte array to read from
	 * @param len amount of bytes to read from the source byte array
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setBytes(int, byte[], int, int)
	 */
	@Override
	public Buffer setBytes(int pos, byte[] b, int offset, int len) {
		buffer.setBytes(pos, b, offset, len);
		return this;
	}

	/**
	 * @param pos position where to write the string
	 * @param str string to write
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setString(int, java.lang.String)
	 */
	@Override
	public Buffer setString(int pos, String str) {
		buffer.setString(pos, str);
		return this;
	}

	/**
	 * Sets the bytes at position {@code pos} in the Buffer to the value of {@code str} encoded in encoding {@code enc}.<p>
	 * The buffer will expand as necessary to accommodate any value written.
	 * @param pos position to start modifying the buffer
	 * @param str value to write in the buffer
	 * @param enc String encoding to use
	 * @return a reference to {@code this} so multiple operations can be appended together.
	 * @see io.vertx.core.buffer.Buffer#setString(int, java.lang.String, java.lang.String)
	 */
	@Override
	public Buffer setString(int pos, String str, String enc) {
		buffer.setString(pos, str, enc);
		return this;
	}

	/**
	 * @return the length of the buffer, measured in bytes.
	 * All positions are indexed from zero.
	 * @see io.vertx.core.buffer.Buffer#length()
	 */
	@Override
	public int length() {
		return buffer.length();
	}

	/**
	 * @return a copy of the entire Buffer.
	 * @see io.vertx.core.buffer.Buffer#copy()
	 */
	@Override
	public Buffer copy() {
		return buffer.copy();
	}

	/**
	 * Returns a slice of this buffer. Modifying the content
	 * of the returned buffer or this buffer affects each other's content
	 * while they maintain separate indexes and marks.
	 * @return a slice of this buffer
	 * @see io.vertx.core.buffer.Buffer#slice()
	 */
	@Override
	public Buffer slice() {
		return buffer.slice();
	}

	/**
	 * Returns a slice of this buffer. Modifying the content
	 * of the returned buffer or this buffer affects each other's content
	 * while they maintain separate indexes and marks.
	 * @param start position to start copy from
	 * @param end position to end copy at
	 * @return  a slice of this buffer
	 * @see io.vertx.core.buffer.Buffer#slice(int, int)
	 */
	@Override
	public Buffer slice(int start, int end) {
		return buffer.slice(start, end);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#toJson()
	 */
	public Object toJson() {
		return buffer.toJson();
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#getDoubleLE(int)
	 */
	public double getDoubleLE(int pos) {
		return buffer.getDoubleLE(pos);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#getFloatLE(int)
	 */
	public float getFloatLE(int pos) {
		return buffer.getFloatLE(pos);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#appendFloatLE(float)
	 */
	public Buffer appendFloatLE(float f) {
		return buffer.appendFloatLE(f);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#appendDoubleLE(double)
	 */
	public Buffer appendDoubleLE(double d) {
		return buffer.appendDoubleLE(d);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#setDoubleLE(int, double)
	 */
	public Buffer setDoubleLE(int pos, double d) {
		return buffer.setDoubleLE(pos, d);
	}

	/**
	 * @see io.vertx.core.buffer.Buffer#setFloatLE(int, float)
	 */
	public Buffer setFloatLE(int pos, float f) {
		return buffer.setFloatLE(pos, f);
	}

}
