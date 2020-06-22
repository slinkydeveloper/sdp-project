package com.slinkydeveloper.sdp.gateway.client.impl;

import com.slinkydeveloper.sdp.gateway.client.GatewayClientService;
import com.slinkydeveloper.sdp.gateway.model.Node;
import com.slinkydeveloper.sdp.gateway.model.SensorDataStatistics;
import com.slinkydeveloper.sdp.log.LoggerConfig;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

public class GatewayClientServiceImpl implements GatewayClientService {

    private final static Logger LOG = LoggerConfig.getLogger(GatewayClientServiceImpl.class);

    private final static Function<String, String> basePath = s -> "/node/" + s;

    private final Client client;
    private final URI host;

    public GatewayClientServiceImpl(Client client, URI host) {
        this.client = client;
        this.host = host;
    }

    @Override
    public Set<Node> nodes() {
        return null;
    }

    @Override
    public SensorDataStatistics data(Integer n) {
        return null;
    }
}
