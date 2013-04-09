/*
 * Copyright (c) 2009 Stephen Tu <stephen_tu@berkeley.edu>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.protobuf.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;

public class NettyRpcClient {

	private final Bootstrap bootstrap;
	private EventLoopGroup group;
	private final NettyRpcClientInitializer clientInitializer = new NettyRpcClientInitializer(
			NettyRpcProto.RpcResponse.getDefaultInstance());

	public NettyRpcClient() {

		this(new NioEventLoopGroup());
	}

	public NettyRpcClient(EventLoopGroup group) {
		this.group = group;
		bootstrap = new Bootstrap();
		bootstrap.group(group);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.handler(clientInitializer);

		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
	}

	public NettyRpcChannel blockingConnect(SocketAddress address)
			throws Exception {

		// Make a new connection.
		ChannelFuture f = bootstrap.connect(address).sync();
		return new NettyRpcChannel(f.awaitUninterruptibly().channel());

	}

	public NettyRpcChannel blockingConnect(String host, int port)
			throws Exception {

		// Make a new connection.
		ChannelFuture future = bootstrap.connect(host, port).sync();
		// ChannelFuture future =bootstrap.connect(host,
		// port).awaitUninterruptibly();

		return new NettyRpcChannel(future.channel());

	}

	public void shutdown() {

		bootstrap.shutdown();
		group.shutdown();
	}

}
