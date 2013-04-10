package com.googlecode.protobuf.netty.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import com.googlecode.protobuf.netty.NettyRpcProto;

public class NettyRpcConnector extends NettyConnector<SocketChannel> {

	private final NettyRpcClientChannelUpstreamHandler handler = new NettyRpcClientChannelUpstreamHandler();
	private final NettyRpcClientInitializer channelInitializer = new NettyRpcClientInitializer(
			handler, NettyRpcProto.RpcResponse.getDefaultInstance());

	public NettyRpcConnector(EventLoopGroup group) {
		super(group);
		handler(channelInitializer);
	}

	public NettyRpcClientChannelUpstreamHandler getHandler() {
		return handler;
	}

}