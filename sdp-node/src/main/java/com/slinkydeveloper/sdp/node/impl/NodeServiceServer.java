package com.slinkydeveloper.sdp.node.impl;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.acquisition.OverlappingSlidingWindowBuffer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class NodeServiceServer {

    private final static Logger LOG = LoggerConfig.getLogger(NodeServiceServer.class);

    private final int port;
    private final Server server;
    private final NodeServiceImpl service;

    public NodeServiceServer(int myId, String myAddress, Map<Integer, String> initialKnownHosts, OverlappingSlidingWindowBuffer<Double> measurementsBuffer) {
        this.port = Integer.parseInt(myAddress.split(Pattern.quote(":"))[1]);
        this.service = new NodeServiceImpl(myId, myAddress, initialKnownHosts, measurementsBuffer);

        this.server = ServerBuilder
                .forPort(this.port)
                .addService(this.service)
                .build();
    }

    /**
     * Start serving requests.
     */
    public void start() throws IOException {
        server.start();
        LOG.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                NodeServiceServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));

        // When we join, we greet the previous node
        this.service.start();
    }

    /**
     * Stop serving requests and shutdown resources
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

}
