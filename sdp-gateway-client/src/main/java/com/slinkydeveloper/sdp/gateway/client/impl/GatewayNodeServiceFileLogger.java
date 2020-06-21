package com.slinkydeveloper.sdp.gateway.client.impl;

import com.slinkydeveloper.sdp.gateway.client.GatewayNodeService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class GatewayNodeServiceFileLogger implements GatewayNodeService {

    private final String filename;

    public GatewayNodeServiceFileLogger(String filename) {
        this.filename = filename;
    }

    @Override
    public Map<Integer, String> join(int myId, String myAddress) {
        return Collections.emptyMap();
    }

    @Override
    public void publishNewAverage(int senderId, Map<Integer, Double> readings) {
        StringBuilder builder = new StringBuilder();
        builder.append("--- publishNewAverage by ")
            .append(senderId)
            .append(" at ")
            .append(System.currentTimeMillis())
            .append(" ---\n");
        readings.forEach((id, v) -> builder.append("Node ").append(id).append(": ").append(v).append('\n'));
        builder.append('\n');
        write(builder.toString());
    }

    @Override
    public void publishNewHosts(int senderId, Map<Integer, String> hosts) {
        StringBuilder builder = new StringBuilder();
        builder.append("--- publishNewHosts by ")
            .append(senderId)
            .append(" at ")
            .append(System.currentTimeMillis())
            .append(" ---\n");
        hosts.forEach((id, v) -> builder.append("Node ").append(id).append(": ").append(v).append('\n'));
        builder.append('\n');
        write(builder.toString());
    }

    private void write(String str) {
        try {
            FileOutputStream out = new FileOutputStream(filename, true);
            out.write(str.getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
