package com.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ClientStreamingServer {
    private static final Logger logger = Logger.getLogger(ClientStreamingServer.class.getName());
    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new ClientStreamingServiceImpl())
                .build()
                .start();
        System.out.println("Starting server. Listening on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    ClientStreamingServer.this.stop();
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
        final ClientStreamingServer server = new ClientStreamingServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class ClientStreamingServiceImpl extends ClientStreamingServiceGrpc.ClientStreamingServiceImplBase {
        @Override
        public StreamObserver<Message> getServerResponse(StreamObserver<Number> responseObserver) {
            System.out.println("Server processing gRPC client-streaming.");
            return new StreamObserver<Message>() {
                private int count = 0;

                @Override
                public void onNext(Message message) {
                    count++;
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Error in client streaming: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onNext(Number.newBuilder().setValue(count).build());
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
