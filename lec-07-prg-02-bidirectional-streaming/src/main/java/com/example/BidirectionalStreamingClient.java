package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BidirectionalStreamingClient {
    private static final Logger logger = Logger.getLogger(BidirectionalStreamingClient.class.getName());
    private final ManagedChannel channel;
    private final BidirectionalServiceGrpc.BidirectionalServiceStub asyncStub;

    public BidirectionalStreamingClient(String host, int port) {
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
                logger.info("Received message from server: " + message.getContent());
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error in bidirectional streaming: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("Stream completed");
                finishLatch.countDown();
            }
        });

        try {
            // Send some messages
            String[] messages = {"Hello", "How are you?", "Goodbye"};
            for (String message : messages) {
                requestObserver.onNext(Message.newBuilder().setContent(message).build());
                Thread.sleep(1000); // Wait a bit between messages
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        BidirectionalStreamingClient client = new BidirectionalStreamingClient("localhost", 50051);
        try {
            client.bidirectionalStream();
        } finally {
            client.shutdown();
        }
    }
}