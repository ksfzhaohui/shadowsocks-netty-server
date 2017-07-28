package org.netty.proxy;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.netty.config.Config;
import org.netty.encryption.CryptFactory;
import org.netty.encryption.CryptUtil;
import org.netty.encryption.ICrypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HostHandler extends ChannelInboundHandlerAdapter {

	private static Log logger = LogFactory.getLog(HostHandler.class);
	private final static int ADDR_TYPE_IPV4 = 1;
	private final static int ADDR_TYPE_HOST = 3;
	private ICrypt _crypt;

	public HostHandler(Config config) {
		this._crypt = CryptFactory.get(config.get_method(), config.get_password());
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buff = (ByteBuf) msg;

		if (buff.readableBytes() <= 0) {
			return;
		}
		ByteBuf dataBuff = Unpooled.buffer();
		dataBuff.writeBytes(CryptUtil.decrypt(_crypt, msg));
		if (dataBuff.readableBytes() < 2) {
			return;
		}
		String host = null;
		int port = 0;
		int addressType = dataBuff.getUnsignedByte(0);
		if (addressType == ADDR_TYPE_IPV4) {
			if (dataBuff.readableBytes() < 7) {
				return;
			}
			dataBuff.readUnsignedByte();
			byte[] ipBytes = new byte[4];
			host = InetAddress.getByAddress(ipBytes).toString().substring(1);
			dataBuff.readBytes(ipBytes);
			port = dataBuff.readShort();
		} else if (addressType == ADDR_TYPE_HOST) {
			int hostLength = dataBuff.getUnsignedByte(1);
			if (dataBuff.readableBytes() < hostLength + 4) {
				return;
			}
			dataBuff.readUnsignedByte();
			dataBuff.readUnsignedByte();
			byte[] hostBytes = new byte[hostLength];
			dataBuff.readBytes(hostBytes);
			host = new String(hostBytes);
			port = dataBuff.readShort();
		} else {
			throw new IllegalStateException("unknown address type: " + addressType);
		}
		logger.info("host = " + host + ",port = " + port);
		ctx.channel().pipeline().addLast(new ClientProxyHandler(host, port, ctx, dataBuff, _crypt));
		ctx.channel().pipeline().remove(this);
	}
}