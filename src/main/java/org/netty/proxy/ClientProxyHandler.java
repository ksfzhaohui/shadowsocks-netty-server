package org.netty.proxy;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.netty.encryption.CryptUtil;
import org.netty.encryption.ICrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 接受客户端代理发送来的消息
 * 
 * @author Administrator
 *
 */
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = LoggerFactory.getLogger(ClientProxyHandler.class);
	private ICrypt _crypt;
	private AtomicReference<Channel> remoteChannel = new AtomicReference<>();
	private ByteBuf clientCache;
	private ChannelHandlerContext clientProxyChannel;

	public ClientProxyHandler(String host, int port, ChannelHandlerContext clientProxyChannel, ByteBuf clientCache,
			ICrypt _crypt) {
		this._crypt = _crypt;
		this.clientCache = clientCache;
		this.clientProxyChannel = clientProxyChannel;
		init(host, port, _crypt);
	}

	/**
	 * 通过host和port与互联网进行连接
	 * 
	 * @param host
	 * @param port
	 * @param clientProxyChannel
	 *            和代理服务器建立的连接
	 * @param _crypt
	 */
	private void init(final String host, final int port, final ICrypt _crypt) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(clientProxyChannel.channel().eventLoop()).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000).option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new IntenetDataHandler(clientProxyChannel, _crypt, clientCache));
					}
				});
		try {
			final InetAddress inetAddress = InetAddress.getByName(host);
			ChannelFuture channelFuture = bootstrap.connect(inetAddress, port);
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						logger.debug(
								"connect success host = " + host + ",port = " + port + ",inetAddress = " + inetAddress);
						remoteChannel.set(future.channel());
					} else {
						logger.debug(
								"connect fail host = " + host + ",port = " + port + ",inetAddress = " + inetAddress);
						future.cancel(true);
						channelClose();
					}
				}
			});
		} catch (Exception e) {
			logger.error("connect intenet error", e);
			channelClose();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buff = (ByteBuf) msg;
		if (buff.readableBytes() <= 0) {
			return;
		}
		byte[] decrypt = CryptUtil.decrypt(_crypt, msg);
		if (remoteChannel.get() != null) {
			remoteChannel.get().writeAndFlush(Unpooled.wrappedBuffer(decrypt));
		} else {
			clientCache.writeBytes(decrypt);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		logger.info("ClientProxyHandler channelInactive close");
		channelClose();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		channelClose();
		logger.error("ClientProxyHandler error", cause);
	}

	private void channelClose() {
		try {
			if (remoteChannel.get() != null) {
				remoteChannel.get().close();
				remoteChannel = null;
			}
			clientProxyChannel.close();
			clientCache.clear();
			clientCache = null;
		} catch (Exception e) {
			logger.error("close channel error", e);
		}
	}
}
