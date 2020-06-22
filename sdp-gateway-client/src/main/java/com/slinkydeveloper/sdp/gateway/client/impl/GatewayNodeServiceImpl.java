package com.slinkydeveloper.sdp.gateway.client.impl;

import com.slinkydeveloper.sdp.gateway.client.GatewayNodeService;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataAverage;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GatewayNodeServiceImpl implements GatewayNodeService {

    private final static Logger LOG = LoggerConfig.getLogger(GatewayNodeServiceImpl.class);

    private final static Function<String, String> basePath = s -> "/node/" + s;

    private final Client client;
    private final URI host;

    public GatewayNodeServiceImpl(Client client, URI host) {
        this.client = client;
        this.host = host;
    }

    @Override
    public Map<Integer, String> join(int myId, String myAddress) {
        Response res = this.client
            .target(host)
            .path(basePath.apply("join"))
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(new Node(myId, myAddress), MediaType.APPLICATION_JSON));
        LOG.info("join response: " + res.getStatus());
        Set<Node> nodes = res.readEntity(new GenericType<Set<Node>>() {
        });

        if (nodes == null) {
            return Collections.emptyMap();
        }

        return nodes
            .stream()
            .filter(n -> n.getId() != myId)
            .collect(Collectors.toMap(Node::getId, Node::getHost));
    }

    @Override
    public void publishNewHosts(int senderId, Map<Integer, String> hosts) {
        Response res = this.client
            .target(host)
            .path(basePath.apply("publishNewHosts"))
            .request()
            .post(Entity.entity(
                hosts
                    .entrySet()
                    .stream()
                    .map(e -> new Node(e.getKey(), e.getValue()))
                    .collect(Collectors.toSet()),
                MediaType.APPLICATION_JSON
            ));
        LOG.info("publishNewHosts response: " + res.getStatus());
    }

    @Override
    public void publishNewAverage(int senderId, Map<Integer, Double> readings) {
        Response res = this.client
            .target(host)
            .path(basePath.apply("publishNewAverage"))
            .request()
            .post(Entity.entity(new SensorDataAverage(
                readings.keySet(),
                readings.values().stream().collect(Collectors.averagingDouble(Double::doubleValue))
            ), MediaType.APPLICATION_JSON));
        LOG.info("publishNewAverage response: " + res.getStatus());
    }
}
