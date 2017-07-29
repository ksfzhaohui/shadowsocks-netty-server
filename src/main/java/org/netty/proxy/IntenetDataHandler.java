package org.netty.proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.netty.encryption.CryptUtil;
import org.netty.encryption.ICrypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 接受互联网消息处理
 * 
 * @author Administrator
 *
 */
public class IntenetDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private static Log logger = LogFactory.getLog(IntenetDataHandler.class);

	private final ChannelHandlerContext clientProxyChannel;
	private ICrypt _crypt;
	private final ByteBuf cacheBuffer;

	public IntenetDataHandler(ChannelHandlerContext clientProxyChannel, ICrypt _crypt, ByteBuf cacheBuffer) {
		this.clientProxyChannel = clientProxyChannel;
		this._crypt = _crypt;
		this.cacheBuffer = cacheBuffer;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.writeAndFlush(cacheBuffer);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
		try {
			byte[] encrypt = CryptUtil.encrypt(_crypt, msg);
			clientProxyChannel.writeAndFlush(Unpooled.copiedBuffer(encrypt));
		} catch (Exception e) {
			ctx.close();
			clientProxyChannel.close();
			logger.error("read intenet message error", e);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		clientProxyChannel.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		clientProxyChannel.close();
	}
}
