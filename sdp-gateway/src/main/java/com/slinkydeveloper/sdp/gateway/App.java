package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.jersey.JacksonObjectMapperProvider;
import io.netty.channel.Channel;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class App {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            System.out.println("Starting Gateway service");

            final Channel server = NettyHttpContainerProvider.createHttp2Server(
                URI.create("http://localhost:" + port + "/"),
                createApp(),
                null
            );
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            System.out.println("Started listening on port " + port);

            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public static ResourceConfig createApp() {
        return new ResourceConfig()
            .register(SseFeature.class)
            .register(JacksonObjectMapperProvider.class)
            .register(EventsResource.class)
            .register(NodeResource.class)
            .register(ClientResource.class)
            .register(JacksonFeature.class);
    }
}
