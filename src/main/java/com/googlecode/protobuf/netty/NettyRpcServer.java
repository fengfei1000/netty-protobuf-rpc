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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.SocketAddress;

import org.apache.log4j.Logger;

import com.google.protobuf.BlockingService;
import com.google.protobuf.Service;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;

public class NettyRpcServer {

	private static final Logger logger = Logger.getLogger(NettyRpcServer.class);
	private EventLoopGroup parentGroup;
	private EventLoopGroup childGroup;
	private final ServerBootstrap bootstrap;
	private final ChannelGroup allChannels = new DefaultChannelGroup();
	private final NettyRpcServerChannelUpstreamHandler handler = new NettyRpcServerChannelUpstreamHandler(
			allChannels);

	private final NettyRpcServerChannelInitializer channelInitializer = new NettyRpcServerChannelInitializer(
			handler, RpcRequest.getDefaultInstance());

	public NettyRpcServer() {
		this(new NioEventLoopGroup(), new NioEventLoopGroup());
	}

	public NettyRpcServer(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
		this.parentGroup = parentGroup;
		this.childGroup = childGroup;
		bootstrap = new ServerBootstrap();
		bootstrap.group(parentGroup, childGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(channelInitializer);
		bootstrap.localAddress(8022);
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
		bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
		bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);

	}

	public void registerService(Service service) {
		handler.registerService(service);
	}

	public void unregisterService(Service service) {
		handler.unregisterService(service);
	}

	public void registerBlockingService(BlockingService service) {
		handler.registerBlockingService(service);
	}

	public void unregisterBlockingService(BlockingService service) {
		handler.unregisterBlockingService(service);
	}

	public void serve() {
		logger.info("Serving...");
		bootstrap.bind();
	}

	public void serve(SocketAddress sa) {
		logger.info("Serving on: " + sa);
		bootstrap.bind(sa);
	}

	public void shutdown() {
		allChannels.close().awaitUninterruptibly();
		bootstrap.shutdown();// releaseExternalResources();
		this.childGroup.shutdown();
		this.parentGroup.shutdown();
	}
}
