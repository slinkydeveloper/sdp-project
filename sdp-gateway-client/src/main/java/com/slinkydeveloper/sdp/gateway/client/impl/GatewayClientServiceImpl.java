package com.slinkydeveloper.sdp.gateway.client.impl;

import com.slinkydeveloper.sdp.gateway.client.GatewayClientService;
import com.slinkydeveloper.sdp.model.NetworkTopologyChangeEvent;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataAverage;
import com.slinkydeveloper.sdp.model.SensorDataStatistics;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class GatewayClientServiceImpl implements GatewayClientService {

    private final static Function<String, String> basePath = s -> "/client/" + s;

    private final Client client;
    private final URI host;

    public GatewayClientServiceImpl(Client client, URI host) {
        this.client = client;
        this.host = host;
    }

    @Override
    public Set<Node> nodes() {
        Response res = this.client
            .target(host)
            .path(basePath.apply("nodes"))
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get();
        return res.readEntity(new GenericType<Set<Node>>() {
        });
    }

    @Override
    public SensorDataStatistics data(Integer limit) {
        WebTarget target = this.client
            .target(host)
            .path(basePath.apply("data"));
        if (limit != null) {
            target = target.queryParam("limit", limit.toString());
        }

        Response res = target
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get();
        return res.readEntity(SensorDataStatistics.class);
    }

    @Override
    public Runnable registerEventHandler(Consumer<NetworkTopologyChangeEvent> networkTopologyChangeEventConsumer, Consumer<SensorDataAverage> newAverageConsumer) {
        WebTarget target = this.client
            .target(host)
            .path("/events");

        SseEventSource sseEventSource = SseEventSource.target(target).build();
        sseEventSource.register(event -> {
            switch (event.getName()) {
                case "newAverage":
                    newAverageConsumer.accept(event.readData(SensorDataAverage.class, MediaType.APPLICATION_JSON_TYPE));
                    break;
                case "networkTopologyChange":
                    networkTopologyChangeEventConsumer.accept(event.readData(NetworkTopologyChangeEvent.class, MediaType.APPLICATION_JSON_TYPE));
                    break;
            }
        });

        sseEventSource.open();

        return sseEventSource::close;
    }
}
