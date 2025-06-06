package com.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BidirectionalStreamingServer {
    private static final Logger logger = Logger.getLogger(BidirectionalStreamingServer.class.getName());
    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new BidirectionalServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    BidirectionalStreamingServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down complete");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final BidirectionalStreamingServer server = new BidirectionalStreamingServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class BidirectionalServiceImpl extends BidirectionalServiceGrpc.BidirectionalServiceImplBase {
        @Override
        public StreamObserver<Message> bidirectionalStream(StreamObserver<Message> responseObserver) {
            return new StreamObserver<Message>() {
                @Override
                public void onNext(Message message) {
                    logger.info("Received message: " + message.getContent());
                    // Echo back the message
                    responseObserver.onNext(Message.newBuilder()
                            .setContent("Server received: " + message.getContent())
                            .build());
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Error in bidirectional streaming: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    logger.info("Stream completed");
                    responseObserver.onCompleted();
                }
            };
        }
    }
}