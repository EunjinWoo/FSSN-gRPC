package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class client {
    private final ManagedChannel channel;
    private final MyServiceGrpc.MyServiceBlockingStub blockingStub;

    public client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = MyServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void myFunction(int number) {
        MyNumber request = MyNumber.newBuilder().setValue(number).build();
        MyNumber response = blockingStub.myFunction(request);
        System.out.println("gRPC result: " + response.getValue());
    }

    public static void main(String[] args) throws Exception {
        client client = new client("localhost", 50051);
        try {
            client.myFunction(4);
        } finally {
            client.shutdown();
        }
    }
}