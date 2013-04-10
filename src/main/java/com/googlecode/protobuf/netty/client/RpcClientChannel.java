package com.googlecode.protobuf.netty.client;

import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;

public abstract class RpcClientChannel implements RpcChannel,
		BlockingRpcChannel {
	public RpcController newRpcController() {
		return new NettyRpcController();
	}

}
