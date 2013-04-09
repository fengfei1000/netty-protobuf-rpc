
# Usage   #
Usage Documentation Updated Dec 27, 2009 by steve33...@gmail.com
## Define Your Service ##
The first step is to define your own service. Read up on http://code.google.com/apis/protocolbuffers/docs/proto.html#services for how to do that. We support both blocking and non blocking services, although it makes more sense for you to use the asynchronous service (non-blocking), since Netty is inherently asynchronous.

## Firing Up Your Server ##
There's only one API class to worry about here, and that's NettyRpcServer. The NettyRpcServer constructor takes a ChannelFactory. You can read more about ChannelFactory at http://www.jboss.org/file-access/default/members/netty/freezone/api/3.0/org/jboss/netty/channel/ChannelFactory.html. Here's an example of how to create a NettyRpcServer, taken from com.googlecode.protobuf.netty.example.CalculatorServer:

	NettyRpcServer server = new NettyRpcServer(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),Executors.newCachedThreadPool()));
Netty developers recommend you use the Executors.newCachedThreadPool(), but you're free to use any type of thread pool you want.

The next step is to register your service(s). There's no limit to how many blocking/non-blocking services your server can be responsible for at once:

	server.registerService(CalcService.newReflectiveService(new CalculatorServiceImpl()));
	server.registerBlockingService(CalcService.newReflectiveBlockingService(new CalculatorServiceImpl()));
As you can see, you can even register the same service as blocking and non-blocking (provided you implement both interfaces).

Once you register all your services, just fire up the server and you're good to go:

	server.serve(new InetSocketAddress(8080));
That's all there is to it! In a few lines of code, you have deployed a multi-threaded, scalable RPC service!

## Using a Service Client ##
RPC servers are no good without any clients. Fortunately, it's very simple to connect to the service you just started. The following examples are taken from com.googlecode.protobuf.netty.example.CalculatorClient.

The main client API class is NettyRpcClient. You instantiate one much in the same fashion as the server:

	NettyRpcClient client = new NettyRpcClient(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),Executors.newCachedThreadPool()));

Now to establish a connection, simply call blockingConnect():

	NettyRpcChannel channel = client.blockingConnect(new InetSocketAddress("localhost", 8080));

A NettyRpcChannel implements RpcChannel as specified by protobuf, for which you can use to create service stubs:

	Stub calcService = CalcService.newStub(channel);
	BlockingInterface blockingCalcService = CalcService.newBlockingStub(channel);

A NettyRpcChannel can also be used to obtain a RpcController:

	RpcController controller = channel.newRpcController();
Right now, the RpcController does not do much more than return error messages, but there are plans to fully implement the cancel operations.

To make an asynchronous request, just do the following:

	// Create the request
	CalcRequest request = CalcRequest.newBuilder().setOp1(15).setOp2(35).build();
	
	// Make the (asynchronous) RPC request
	calcService.add(controller, request, new RpcCallback<CalcResponse>() {
	    public void run(CalcResponse response) {
	        if (response != null) {
	            System.out.println("The answer is: " + response.getResult());
	        } else {
	            System.out.println("Oops, there was an error: " + controller.errorText());
	        }
	    }
	});

You make a blocking request by:

	try {
	    CalcResponse response = blockingCalcService.multiply(controller, request);
	    if (response != null) {
	        System.out.println("The answer is: " + response.getResult());
	    } else {
	        System.out.println("Oops, there was an error: " + controller.errorText());
	    }
	} catch (ServiceException e) {
	    e.printStackTrace();
	}

Note. Since Netty is inherently asynchronous, blocking RPC calls are still implemented asynchronously. You can think of a Netty blocking call as simply an asynchronous call, with a callback that puts the current thread to sleep until the call has completed.

Once you are done with requests, be sure to close the connection:

    // Close the channel
    channel.close();
    
    // Close the client
    client.shutdown();
And that's all there is to it!