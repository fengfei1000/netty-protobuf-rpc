/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.googlecode.protobuf.netty;

import com.google.protobuf.Message;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * Creates a newly configured {@link ChannelPipeline} for a client-side channel.
 */
public class NettyRpcClientInitializer extends
		ChannelInitializer<SocketChannel> {
	// Since Netty 3.2.7.Final, it's safe to use Integer.MAX_VALUE
	// For more information see:
	// http://stackoverflow.com/questions/8065022/how-to-use-unlimited-frame-sizes-in-jboss-netty-without-wasting-memory
	private static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

	private final Message defaultInstance;

	NettyRpcClientInitializer(Message defaultInstance) {

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

		p.addLast("handler", new NettyRpcClientChannelUpstreamHandler());
	}
}
