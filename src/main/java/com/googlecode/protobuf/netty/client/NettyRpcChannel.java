package com.googlecode.protobuf.netty.client;

import io.netty.channel.Channel;

import org.apache.log4j.Logger;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;

public class NettyRpcChannel extends RpcClientChannel {

	private static final Logger logger = Logger
			.getLogger(NettyRpcChannel.class);

	private final Channel channel;
	private final NettyRpcClientChannelUpstreamHandler handler;

	public NettyRpcChannel(Channel channel) {
		this.channel = channel;
		this.handler = channel.pipeline().get(
				NettyRpcClientChannelUpstreamHandler.class);
		if (handler == null) {
			throw new IllegalArgumentException(
					"Channel does not have proper handler");
		}
	}

	public Channel getChannel() {
		return channel;
	}

	public void callMethod(MethodDescriptor method, RpcController controller,
			Message request, Message responsePrototype,
			RpcCallback<Message> done) {
		int nextSeqId = (done == null) ? -1 : handler.getNextSeqId();
		Message rpcRequest = buildRequest(done != null, nextSeqId, false,
				method, request);
		if (done != null) {
			handler.registerCallback(nextSeqId,
					new ResponsePrototypeRpcCallback(controller,
							responsePrototype, done));
		}
		channel.write(rpcRequest);
	}

	public Message callBlockingMethod(MethodDescriptor method,
			RpcController controller, Message request, Message responsePrototype)
			throws ServiceException {
		logger.debug("calling blocking method: " + method.getFullName());
		BlockingRpcCallback callback = new BlockingRpcCallback();
		ResponsePrototypeRpcCallback rpcCallback = new ResponsePrototypeRpcCallback(
				controller, responsePrototype, callback);
		int nextSeqId = handler.getNextSeqId();
		Message rpcRequest = buildRequest(true, nextSeqId, true, method,
				request);
		handler.registerCallback(nextSeqId, rpcCallback);
		channel.write(rpcRequest);
		synchronized (callback) {
			while (!callback.isDone()) {
				try {
					callback.wait();
				} catch (InterruptedException e) {
					logger.warn("Interrupted while blocking", e);
					/* Ignore */
				}
			}
		}
		if (rpcCallback.getRpcResponse() != null
				&& rpcCallback.getRpcResponse().hasErrorCode()) {
			// TODO: should we only throw this if the error code matches the
			// case where the server call threw a ServiceException?
			throw new ServiceException(rpcCallback.getRpcResponse()
					.getErrorMessage());
		}
		return callback.getMessage();
	}

	public void close() {
		channel.close().awaitUninterruptibly();
	}

	private Message buildRequest(boolean hasSequence, int seqId,
			boolean isBlocking, MethodDescriptor method, Message request) {
		RpcRequest.Builder requestBuilder = RpcRequest.newBuilder();
		if (hasSequence) {
			requestBuilder.setId(seqId);
		}
		return requestBuilder.setIsBlockingService(isBlocking)
				.setServiceName(method.getService().getFullName())
				.setMethodName(method.getName())
				.setRequestMessage(request.toByteString()).build();
	}

	private static class BlockingRpcCallback implements RpcCallback<Message> {

		private boolean done = false;
		private Message message;

		public void run(Message message) {
			this.message = message;
			synchronized (this) {
				done = true;
				notify();
			}
		}

		public Message getMessage() {
			return message;
		}

		public boolean isDone() {
			return done;
		}

	}

}
