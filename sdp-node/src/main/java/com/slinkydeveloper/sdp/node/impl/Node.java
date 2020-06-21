package com.slinkydeveloper.sdp.node.impl;

import com.slinkydeveloper.sdp.gateway.impl.GatewayServiceFileLogger;
import com.slinkydeveloper.sdp.node.acquisition.OverlappingSlidingWindowBuffer;
import com.slinkydeveloper.sdp.node.simulator.PM10Simulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Node {

    public static void main(String[] args) throws InterruptedException, IOException {
        int myId = Integer.parseInt(args[0]);
        String myAddress = args[1];

        Map<Integer, String> initialKnownHosts = new HashMap<>();
        if (args.length == 3) {
            for (String entry : args[2].split(Pattern.quote(","))) {
                String[] splitted = entry.split(Pattern.quote("="));
                initialKnownHosts.put(
                    Integer.parseInt(splitted[0]),
                    splitted[1]
                );
            }
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
            new GatewayServiceFileLogger("gateway.txt")
        );
        serviceServer.start();
        serviceServer.blockUntilShutdown();
    }

}
