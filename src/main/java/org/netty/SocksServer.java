package org.netty;

import org.netty.config.Config;
import org.netty.config.ConfigXmlLoader;
import org.netty.proxy.HostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SocksServer {

	private static Logger logger = LoggerFactory.getLogger(SocksServer.class);

	private static final String CONFIG = "conf/config.xml";

	private EventLoopGroup bossGroup = null;
	private EventLoopGroup workerGroup = null;
	private ServerBootstrap bootstrap = null;
	private static SocksServer socksServer = new SocksServer();

	public static SocksServer getInstance() {
		return socksServer;
	}

	private SocksServer() {

	}

	public void start() {
		try {
			final Config config = ConfigXmlLoader.load(CONFIG);

			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();
			bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_KEEPALIVE, true).childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel socketChannel) throws Exception {
							socketChannel.pipeline().addLast(new HostHandler(config));
						}
					});

			logger.info("Start At Port " + config.get_localPort());
			bootstrap.bind(config.get_localPort()).sync().channel().closeFuture().sync();
		} catch (Exception e) {
			logger.error("start error", e);
		} finally {
			stop();
		}
	}

	public void stop() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully();
		}
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
		}
		logger.info("Stop Server!");
	}

}
