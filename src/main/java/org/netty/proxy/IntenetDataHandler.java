package org.netty.proxy;

import org.netty.encryption.CryptUtil;
import org.netty.encryption.ICrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static Logger logger = LoggerFactory.getLogger(IntenetDataHandler.class);

	private final ChannelHandlerContext clientProxyChannel;
	private ICrypt _crypt;
	private ByteBuf cacheBuffer;

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
			clientProxyChannel.writeAndFlush(Unpooled.wrappedBuffer(encrypt));
		} catch (Exception e) {
			ctx.close();
			channelClose();
			logger.error("read intenet message error", e);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		logger.info("IntenetDataHandler channelInactive close");
		channelClose();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		channelClose();
		logger.error("IntenetDataHandler error", cause);
	}

	private void channelClose() {
		try {
			clientProxyChannel.close();
			cacheBuffer.clear();
			cacheBuffer = null;
		} catch (Exception e) {
			logger.error("close channel error", e);
		}
	}
}
