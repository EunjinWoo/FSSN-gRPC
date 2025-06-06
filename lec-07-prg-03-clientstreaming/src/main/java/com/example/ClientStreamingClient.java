package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ClientStreamingClient {
    private static final Logger logger = Logger.getLogger(ClientStreamingClient.class.getName());
    private final ManagedChannel channel;
    private final ClientStreamingServiceGrpc.ClientStreamingServiceStub asyncStub;

    public ClientStreamingClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = ClientStreamingServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void clientStream() throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<Message> requestObserver = asyncStub.getServerResponse(new StreamObserver<Number>() {
            @Override
            public void onNext(Number number) {
                System.out.println("[server to client] " + number.getValue());
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error in client streaming: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });

        try {
            // Send messages exactly like Python example
            String[] messages = {
                    "message #1",
                    "message #2",
                    "message #3",
                    "message #4",
                    "message #5"
            };

            for (String message : messages) {
                System.out.println("[client to server] " + message);
                requestObserver.onNext(Message.newBuilder().setMessage(message).build());
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        ClientStreamingClient client = new ClientStreamingClient("localhost", 50051);
        try {
            client.clientStream();
        } finally {
            client.shutdown();
        }
    }
}
