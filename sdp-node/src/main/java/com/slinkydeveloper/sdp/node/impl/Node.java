package com.slinkydeveloper.sdp.node.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Node {

    public static void main(String[] args) throws InterruptedException, IOException {
        int myId = Integer.parseInt(args[0]);
        String myAddress = args[1];

        Map<Integer, String> initialKnownHosts = new HashMap<>();
        for (String entry : args[2].split(Pattern.quote(","))) {
            String[] splitted = entry.split(Pattern.quote("="));
            initialKnownHosts.put(
                    Integer.parseInt(splitted[0]),
                    splitted[1]
            );
        }

        NodeServiceServer serviceServer = new NodeServiceServer(myId, myAddress, initialKnownHosts);
        serviceServer.start();
        serviceServer.blockUntilShutdown();
    }

}
