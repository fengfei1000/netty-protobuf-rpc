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
package com.googlecode.protobuf.netty.client;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;
@Sharable
public class NettyRpcClientChannelUpstreamHandler extends
		ChannelInboundMessageHandlerAdapter<RpcResponse> {

	private static final Logger logger = Logger
			.getLogger(NettyRpcClientChannelUpstreamHandler.class);

	private final AtomicInteger seqNum = new AtomicInteger(0);

	private final Map<Integer, ResponsePrototypeRpcCallback> callbackMap = new ConcurrentHashMap<Integer, ResponsePrototypeRpcCallback>();

	public int getNextSeqId() {
		return seqNum.getAndIncrement();
	}

	public synchronized void registerCallback(int seqId,
			ResponsePrototypeRpcCallback callback) {
		if (callbackMap.containsKey(seqId)) {
			throw new IllegalArgumentException("Callback already registered");
		}
		callbackMap.put(seqId, callback);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.info("Channel connected");
	}

	// @Override
	// public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent
	// e) {
	//
	// }

	@Override
	public void messageReceived(ChannelHandlerContext ctx, RpcResponse response)
			throws Exception {

		// RpcResponse response = (RpcResponse) e.getMessage();

		if (!response.hasId()) {
			logger.debug("Should never receive response without seqId");
			return;
		}

		int seqId = response.getId();
		ResponsePrototypeRpcCallback callback = callbackMap.remove(seqId);

		if (response.hasErrorCode() && callback != null
				&& callback.getRpcController() != null) {
			callback.getRpcController().setFailed(response.getErrorMessage());
		}

		if (callback == null) {
			logger.debug("Received response with no callback registered");
		} else {
			logger.debug("Invoking callback with response");
			callback.run(response);
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.error("Unhandled exception in handler", cause);
		ctx.channel().close();
		// ctx.close();
		throw new Exception(cause);
	}

}
