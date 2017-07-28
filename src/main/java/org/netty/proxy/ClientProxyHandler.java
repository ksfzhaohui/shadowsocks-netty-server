package org.netty.proxy;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.netty.encryption.CryptUtil;
import org.netty.encryption.ICrypt;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 接受客户端代理发送来的消息
 * 
 * @author Administrator
 *
 */
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

	private static Log logger = LogFactory.getLog(ClientProxyHandler.class);
	private ICrypt _crypt;
	private AtomicReference<Channel> remoteChannel = new AtomicReference<>();
	private ByteBuf clientCache;

	public ClientProxyHandler(String host, int port, ChannelHandlerContext clientCtx, ByteBuf clientCache,
			ICrypt _crypt) {
		this._crypt = _crypt;
		this.clientCache = clientCache;
		init(host, port, clientCtx, clientCache, _crypt);
	}

	private void init(final String host, final int port, final ChannelHandlerContext clientCtx,
			final ByteBuf clientCache, final ICrypt _crypt) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(clientCtx.channel().eventLoop()).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new IntenetDataHandler(clientCtx, _crypt, clientCache));
					}
				});
		try {
			ChannelFuture channelFuture = bootstrap.connect(InetAddress.getByName(host), port);
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						logger.info("connect success host = " + host + ",port = " + port);
						remoteChannel.set(future.channel());
					} else {
						logger.info("connect fail host = " + host + ",port = " + port);
						clientCtx.close();
					}
				}
			});
		} catch (Exception e) {
			logger.error("connect intenet error", e);
			clientCtx.close();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buff = (ByteBuf) msg;
		if (buff.readableBytes() <= 0) {
			return;
		}
		byte[] decrypt = CryptUtil.decrypt(_crypt, msg);
		if (remoteChannel.get() == null) {
			clientCache.writeBytes(decrypt);
		} else {
			remoteChannel.get().writeAndFlush(Unpooled.copiedBuffer(decrypt));
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		if (remoteChannel.get() != null) {
			remoteChannel.get().close();
		}
	}

	/**
	 * 接受互联网消息处理
	 * 
	 * @author Administrator
	 *
	 */
	public static class IntenetDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

		private final ChannelHandlerContext clientCtx;
		private ICrypt _crypt;
		private final ByteBuf byteBuffer;

		public IntenetDataHandler(ChannelHandlerContext clientCtx, ICrypt _crypt, ByteBuf byteBuffer) {
			this.clientCtx = clientCtx;
			this._crypt = _crypt;
			this.byteBuffer = byteBuffer;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ctx.writeAndFlush(byteBuffer);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
			try {
				byte[] encrypt = CryptUtil.encrypt(_crypt, msg);
				clientCtx.writeAndFlush(Unpooled.copiedBuffer(encrypt));
			} catch (Exception e) {
				ctx.close();
				clientCtx.close();
				logger.error("read intenet message error", e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			ctx.close();
			clientCtx.close();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			ctx.close();
			clientCtx.close();
		}
	}
}
