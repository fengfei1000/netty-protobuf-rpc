package com.googlecode.protobuf.netty.client;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import fengfei.forest.slice.OverflowType;
import fengfei.forest.slice.Resource;
import fengfei.forest.slice.Router;
import fengfei.forest.slice.SelectType;
import fengfei.forest.slice.SliceResource;
import fengfei.forest.slice.impl.AccuracyRouter;
import fengfei.forest.slice.server.pool.CommonsPoolableObjectFactory;
import fengfei.forest.slice.server.pool.CommonsPoolableSourceFactory;
import fengfei.forest.slice.server.pool.PoolableException;
import fengfei.forest.slice.server.pool.PoolableServerRouter;
import fengfei.forest.slice.server.pool.PoolableSourceFactory;
import fengfei.forest.slice.server.pool.PooledSource;

public class NettyClusterClient {
	private static final Logger log = Logger.getLogger(NettyClusterClient.class);
	public static final String SPLIT_REG = "&|,|\\||，|	|\n|\r|\n\r";
	private PoolableServerRouter<Long, Channel> router;
	NettyRpcClientChannelUpstreamHandler handler;
	CommonsPoolableObjectFactory<Channel> poolableObjectFactory;
	PoolableSourceFactory<Channel> poolableSourceFactory;
	NettyRpcConnector connector;

	private NettyClusterClient(EventLoopGroup group, String hosts) {
		Router<Long> facade = new AccuracyRouter<Long>();
		connector = new NettyRpcConnector(group);
		this.handler = connector.getHandler();
		poolableObjectFactory = new SharableClientPoolableFactory(connector);
		poolableSourceFactory = new CommonsPoolableSourceFactory<Channel>(
				poolableObjectFactory);
		router = new PoolableServerRouter<Long, Channel>(facade,
				poolableSourceFactory);

		router.setSelectType(SelectType.Loop);
		router.setOverflowType(OverflowType.First);
		// add host
		String[] hs = hosts.split(SPLIT_REG);
		for (int i = 0; i < hs.length; i++) {
			String[] host = hs[i].split(":");
			String name = host[0];
			String port = host[1];
			SliceResource resource = new SliceResource(new Resource(hs[i]));
			resource.addParam("host", name);
			resource.addParam("port", port);
			router.register((i + 1l), resource);
		}

	}

	public static NettyClusterClient create(String hosts) {
		return create(new NioEventLoopGroup(), hosts);
	}

	public static NettyClusterClient create(EventLoopGroup group, String hosts) {

		return new NettyClusterClient(group, hosts);
	}

	public void setHandler(NettyRpcClientChannelUpstreamHandler handler) {
		this.handler = handler;
	}

	public NettyRpcClientChannelUpstreamHandler getHandler() {
		return handler;
	}

	public NettyRpcCLusterChannel newCLusterChannel() {
		NettyRpcCLusterChannel channel = new NettyRpcCLusterChannel(router,
				getHandler());
		return channel;

	}

	public void shutdown() {
		Set<Entry<String, PooledSource<Channel>>> entries = router
				.getPooledDataSources().entrySet();
		for (Entry<String, PooledSource<Channel>> entry : entries) {
			try {
				entry.getValue().close();
			} catch (PoolableException e) {
				log.error("close source error for " + entry.getKey(), e);
			}
		}
		router = null;
	}

	static protected class SharableClientPoolableFactory extends
			CommonsPoolableObjectFactory<Channel> {
		private NettyRpcConnector connector;

		public SharableClientPoolableFactory(NettyRpcConnector connector) {
			super();
			this.connector = connector;
		}

		@Override
		public Channel makeObject() throws Exception {
			return connector.blockingConnect(host, port);
		}

		@Override
		public void destroyObject(Channel rpcChannel) throws Exception {
			rpcChannel.close();
		}

		@Override
		public boolean validateObject(Channel channel) {
			return channel != null && channel.isActive() && channel.isOpen();
		}

		@Override
		public void activateObject(Channel channel) throws Exception {
			channel.connect(new InetSocketAddress(host, port))
					.awaitUninterruptibly();
		}

		@Override
		public void passivateObject(Channel channel) throws Exception {
			channel.close().sync();
		}

	}

}
