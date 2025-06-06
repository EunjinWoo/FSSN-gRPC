package com.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerStreamingServer {
    private static final Logger logger = Logger.getLogger(ServerStreamingServer.class.getName());
    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new ServerStreamingServiceImpl())
                .build()
                .start();
        System.out.println("Starting server. Listening on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    ServerStreamingServer.this.stop();
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
        final ServerStreamingServer server = new ServerStreamingServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class ServerStreamingServiceImpl extends ServerStreamingServiceGrpc.ServerStreamingServiceImplBase {
        @Override
        public void getServerResponse(Number request, StreamObserver<Message> responseObserver) {
            System.out.println("Server processing gRPC server-streaming {" + request.getValue() + "}.");

            String[] messages = {
                    "message #1",
                    "message #2",
                    "message #3",
                    "message #4",
                    "message #5"
            };

            for (String message : messages) {
                responseObserver.onNext(Message.newBuilder().setMessage(message).build());
            }
            responseObserver.onCompleted();
        }
    }
}
