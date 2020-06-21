package com.slinkydeveloper.sdp.node.impl;

import com.slinkydeveloper.sdp.gateway.client.GatewayNodeService;
import com.slinkydeveloper.sdp.gateway.client.impl.GatewayNodeServiceFileLogger;
import com.slinkydeveloper.sdp.gateway.client.impl.GatewayNodeServiceImpl;
import com.slinkydeveloper.sdp.jersey.JerseyUtils;
import com.slinkydeveloper.sdp.node.acquisition.OverlappingSlidingWindowBuffer;
import com.slinkydeveloper.sdp.node.simulator.PM10Simulator;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Node {

    public static void main(String[] args) throws InterruptedException, IOException {
        int myId = Integer.parseInt(args[0]);
        String myAddress = args[1];

        GatewayNodeService gatewayNodeService;
        Map<Integer, String> initialKnownHosts;

        String mode = args[2];
        switch (mode) {
            case "-m":
            case "--mock":
                initialKnownHosts = new HashMap<>();
                if (args.length == 4) {
                    for (String entry : args[3].split(Pattern.quote(","))) {
                        String[] splitted = entry.split(Pattern.quote("="));
                        initialKnownHosts.put(
                            Integer.parseInt(splitted[0]),
                            splitted[1]
                        );
                    }
                }
                gatewayNodeService = new GatewayNodeServiceFileLogger("gateway.txt");
                break;
            case "-g":
            case "--gateway":
                gatewayNodeService = new GatewayNodeServiceImpl(JerseyUtils.createClient(), URI.create(args[3]));
                initialKnownHosts = gatewayNodeService.join(myId, myAddress);
                break;
            default:
                throw new IllegalArgumentException("Mode '" + mode + "' not recognized");
        }

        // Start the measurements simulator
        OverlappingSlidingWindowBuffer<Double> buffer = new OverlappingSlidingWindowBuffer<>(10, 0.5, OverlappingSlidingWindowBuffer.AVERAGE_REDUCER);
        PM10Simulator simulator = new PM10Simulator(buffer);
        simulator.setDaemon(true);
        simulator.start();

        NodeServiceServer serviceServer = new NodeServiceServer(
            myId,
            myAddress,
            initialKnownHosts,
            buffer,
            gatewayNodeService
        );
        serviceServer.start();
        serviceServer.blockUntilShutdown();
    }

}
