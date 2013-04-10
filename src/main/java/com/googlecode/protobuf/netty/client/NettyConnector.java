package com.googlecode.protobuf.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyConnector<C extends Channel> {

	protected final Bootstrap bootstrap;
	protected Channel channel;

	public NettyConnector(EventLoopGroup group) {
		bootstrap = new Bootstrap();
		bootstrap.group(group);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);

	}

	public void handler(ChannelInitializer<C> channelInitializer) {
		bootstrap.handler(channelInitializer);
	}

	public Channel blockingConnect(String host, int port) throws Exception {
		if (channel == null) {
			// Make a new connection.
			ChannelFuture future = bootstrap.connect(host, port).sync();
			//
			channel = future.channel();
		}

		return channel;

	}

	public void shutdown() {
		try {
			channel.close().sync();
			bootstrap.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}