package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.model.NetworkTopologyChangeEvent;
import com.slinkydeveloper.sdp.model.SensorDataAverage;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.Objects;

@Singleton
@Path("events")
public class EventsResource {

    private final Sse sse;
    private final SseBroadcaster broadcaster;

    public EventsResource(@Context Sse sse) {
        Objects.requireNonNull(sse);
        this.sse = sse;
        this.broadcaster = sse.newBroadcaster();
    }

    public void sendMessage(SensorDataAverage average) {
        final OutboundSseEvent event = sse.newEventBuilder()
            .name("newAverage")
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(average)
            .build();

        broadcaster.broadcast(event);
    }

    public void sendMessage(NetworkTopologyChangeEvent networkTopologyChangeEvent) {
        final OutboundSseEvent event = sse.newEventBuilder()
            .name("networkTopologyChange")
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(networkTopologyChangeEvent)
            .build();

        broadcaster.broadcast(event);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenToBroadcast(@Context SseEventSink eventSink) {
        this.broadcaster.register(eventSink);
    }
}
