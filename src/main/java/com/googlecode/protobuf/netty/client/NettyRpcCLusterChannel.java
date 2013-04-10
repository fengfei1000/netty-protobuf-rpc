package com.googlecode.protobuf.netty.client;

import io.netty.channel.Channel;

import org.apache.log4j.Logger;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;

import fengfei.forest.slice.server.pool.PoolableException;
import fengfei.forest.slice.server.pool.PoolableServerResource;
import fengfei.forest.slice.server.pool.PoolableServerRouter;
import fengfei.forest.slice.server.pool.PooledSource;

public class NettyRpcCLusterChannel extends RpcClientChannel {

	private static final Logger logger = Logger
			.getLogger(NettyRpcCLusterChannel.class);

	private final PoolableServerRouter<Long, Channel> router;
	private final NettyRpcClientChannelUpstreamHandler handler;

	public NettyRpcCLusterChannel(PoolableServerRouter<Long, Channel>  router,
			NettyRpcClientChannelUpstreamHandler handler) {
		this.router = router;
		this.handler = handler;
		if (handler == null) {
			throw new IllegalArgumentException(
					"Channel does not have proper handler");
		}
	}

	public PoolableServerRouter<Long, Channel> getRouter() {
		return router;
	}

	public PooledSource<Channel> getPooledSource(Long key) {
		PoolableServerResource<Channel> resource = router.locate(key);
		System.out.println(resource);
		return resource.getPooledSource();
	}

	public RpcController newRpcController() {
		return new NettyRpcController();
	}

	public void write(Object message) throws PoolableException {
		PooledSource<Channel> ps = null;
		Channel channel = null;
		try {
			ps = getPooledSource(0l);
			System.out.println("===" + ps);
			channel = ps.getSource();
			channel.write(message);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ps.close(channel);
		}
	}

	public void callMethod(MethodDescriptor method, RpcController controller,
			Message request, Message responsePrototype,
			RpcCallback<Message> done) {
		int nextSeqId = (done == null) ? -1 : handler.getNextSeqId();
		Message rpcRequest = buildRequest(done != null, nextSeqId, false,
				method, request);
		if (done != null) {
			ResponsePrototypeRpcCallback callback = new ResponsePrototypeRpcCallback(
					controller, responsePrototype, done);
			handler.registerCallback(nextSeqId, callback);
		}
		try {
			write(rpcRequest);
		} catch (PoolableException e) {
			throw new RuntimeException(
					"request error for call service method.", e);
		}

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
		try {
			write(rpcRequest);
		} catch (PoolableException e) {
			throw new RuntimeException(
					"request error for call service method.", e);
		}
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
