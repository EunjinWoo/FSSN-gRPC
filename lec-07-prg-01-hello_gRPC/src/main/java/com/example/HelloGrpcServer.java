package com.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HelloGrpcServer {
    private static final Logger logger = Logger.getLogger(HelloGrpcServer.class.getName());
    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new MyServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    HelloGrpcServer.this.stop();
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
        final HelloGrpcServer server = new HelloGrpcServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class MyServiceImpl extends MyServiceGrpc.MyServiceImplBase {
        @Override
        public void myFunction(MyNumber request, StreamObserver<MyNumber> responseObserver) {
            MyNumber response = MyNumber.newBuilder()
                    .setValue(MyFunction.myFunc(request.getValue()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}