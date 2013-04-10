package com.googlecode.protobuf.netty.client;

import org.apache.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;

public class ResponsePrototypeRpcCallback implements RpcCallback<RpcResponse> {
	private static final Logger logger = Logger
			.getLogger(ResponsePrototypeRpcCallback.class);
	private final RpcController controller;
	private final Message responsePrototype;
	private final RpcCallback<Message> callback;

	private RpcResponse rpcResponse;

	public ResponsePrototypeRpcCallback(RpcController controller,
			Message responsePrototype, RpcCallback<Message> callback) {
		if (responsePrototype == null) {
			throw new IllegalArgumentException(
					"Must provide response prototype");
		} else if (callback == null) {
			throw new IllegalArgumentException("Must provide callback");
		}
		this.controller = controller;
		this.responsePrototype = responsePrototype;
		this.callback = callback;
	}

	public void run(RpcResponse message) {
		rpcResponse = message;
		try {
			Message response = (message == null || !message
					.hasResponseMessage()) ? null : responsePrototype
					.newBuilderForType()
					.mergeFrom(message.getResponseMessage()).build();
			callback.run(response);
		} catch (InvalidProtocolBufferException e) {
			logger.warn("Could not marshall into response", e);
			if (controller != null) {
				controller
						.setFailed("Received invalid response type from server");
			}
			callback.run(null);
		}
	}

	public RpcController getRpcController() {
		return controller;
	}

	public RpcResponse getRpcResponse() {
		return rpcResponse;
	}

}