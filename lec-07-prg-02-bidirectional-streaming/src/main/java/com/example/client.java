package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class client {
    private static final Logger logger = Logger.getLogger(client.class.getName());
    private final ManagedChannel channel;
    private final BidirectionalServiceGrpc.BidirectionalServiceStub asyncStub;

    public client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = BidirectionalServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void bidirectionalStream() throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<Message> requestObserver = asyncStub.bidirectionalStream(new StreamObserver<Message>() {
            @Override
            public void onNext(Message message) {
                System.out.println("[server to client] " + message.getContent());
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error in bidirectional streaming: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });

        try {
            String[] messages = {
                    "message #1",
                    "message #2",
                    "message #3",
                    "message #4",
                    "message #5"
            };

            for (String message : messages) {
                System.out.println("[client to server] " + message);
                requestObserver.onNext(Message.newBuilder().setContent(message).build());
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        client client = new client("localhost", 50051);
        try {
            client.bidirectionalStream();
        } finally {
            client.shutdown();
        }
    }
}
