
fork by [https://code.google.com/p/netty-protobuf-rpc/](https://code.google.com/p/netty-protobuf-rpc/)

# Introduction #
This project expands on Netty's support for Google's Protocol Buffers and implements an RPC layer for Protocol Buffer Services. It is still in the experimental phase.

Installation
If you want to compile from source, I've only set this up using Maven. First, you'll have to have protobuf's java library installed in your local repo, which you can do by downloading protobuf and running mvn install in the java directory. Then you can just run mvn install in the netty-protobuf-rpc directory.

If you just want a jar, check the Downloads section.

#Usage#
Visit the [Usage](Usage.md) page for more information on using the code, or read on further for some background and more information.

# Background #
Netty is a pretty nifty framework for easily creating very scalable client/server communications. It is based on the Java NIO library, and seems to perform pretty well from what I have read. Additionally, the developers recently released decoders/encoders for Protocol Buffers, making it very easy to send a user-defined Message across the network in their framework.

As you can see from http://code.google.com/p/protobuf/wiki/ThirdPartyAddOns#RPC_Implementations, there are already quite a few RPC implementations out there. It was very surprising to me that there was not already an RPC implementation that used Netty under its covers. Netty essentially does all of the hard work under the hood for asynchronous network communication, and it is just a matter of putting the pieces together!

So I figured that I'd give it a shot, and hence this project was born.

# Contribute #
This is still very experimental, and there's a lot of things I'd like to work on but don't have the time. If you feel like you want to contribute, please get in contact with me! Incomplete items are:

Fully implementing RpcController's cancel operations
Unit Tests
Benchmark Tests
Better delivery of remote exceptions to client
