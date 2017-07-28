package org.netty.encryption;

import java.io.ByteArrayOutputStream;

import io.netty.buffer.ByteBuf;

public class CryptUtil {

	public static final int BUFFER_SIZE = 16384;
	private static ByteArrayOutputStream _remoteOutStream;
	private static ByteArrayOutputStream _localOutStream;

	static {
		_remoteOutStream = new ByteArrayOutputStream(BUFFER_SIZE);
		_localOutStream = new ByteArrayOutputStream(BUFFER_SIZE);
	}

	public static byte[] encrypt(ICrypt crypt, Object msg) {
		byte[] data = null;
		ByteBuf bytebuff = (ByteBuf) msg;
		if (!bytebuff.hasArray()) {
			int len = bytebuff.readableBytes();
			byte[] arr = new byte[len];
			bytebuff.getBytes(0, arr);
			crypt.encrypt(arr, arr.length, _remoteOutStream);
			data = _remoteOutStream.toByteArray();
		}
		return data;
	}

	public static byte[] decrypt(ICrypt crypt, Object msg) {
		byte[] data = null;
		ByteBuf bytebuff = (ByteBuf) msg;
		if (!bytebuff.hasArray()) {
			int len = bytebuff.readableBytes();
			byte[] arr = new byte[len];
			bytebuff.getBytes(0, arr);
			crypt.decrypt(arr, arr.length, _localOutStream);
			data = _localOutStream.toByteArray();
		}
		return data;
	}

}
