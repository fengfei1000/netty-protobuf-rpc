package com.googlecode.protobuf.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import com.google.protobuf.Message;

public class NettyRpcServerChannelInitializer  extends
		ChannelInitializer<SocketChannel> {
	// Since Netty 3.2.7.Final, it's safe to use Integer.MAX_VALUE
	// For more information see:
	// http://stackoverflow.com/questions/8065022/how-to-use-unlimited-frame-sizes-in-jboss-netty-without-wasting-memory
	private static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;
	private final  NettyRpcServerChannelUpstreamHandler handler;
	private final Message defaultInstance;

	NettyRpcServerChannelInitializer(
			 NettyRpcServerChannelUpstreamHandler handler,
			Message defaultInstance) {
		this.handler = handler;
		this.defaultInstance = defaultInstance;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {

		ChannelPipeline p = ch.pipeline();
		p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
				MAX_FRAME_BYTES_LENGTH, 0, 4, 0, 4));
		p.addLast("protobufDecoder", new ProtobufDecoder(defaultInstance));

		p.addLast("frameEncoder", new LengthFieldPrepender(4));
		p.addLast("protobufEncoder", new ProtobufEncoder());

		p.addLast("handler", handler);
	}

}
