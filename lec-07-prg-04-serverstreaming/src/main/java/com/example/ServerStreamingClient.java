package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerStreamingClient {
    private static final Logger logger = Logger.getLogger(ServerStreamingClient.class.getName());
    private final ManagedChannel channel;
    private final ServerStreamingServiceGrpc.ServerStreamingServiceBlockingStub blockingStub;

    public ServerStreamingClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = ServerStreamingServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void serverStream() {
        Number request = Number.newBuilder().setValue(5).build();
        blockingStub.getServerResponse(request).forEachRemaining(response -> {
            System.out.println("[server to client] " + response.getMessage());
        });
    }

    public static void main(String[] args) throws Exception {
        ServerStreamingClient client = new ServerStreamingClient("localhost", 50051);
        try {
            client.serverStream();
        } finally {
            client.shutdown();
        }
    }
}
