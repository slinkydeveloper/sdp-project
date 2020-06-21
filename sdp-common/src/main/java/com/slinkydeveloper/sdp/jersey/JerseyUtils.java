package com.slinkydeveloper.sdp.jersey;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class JerseyUtils {

    public static Client createClient() {
        return ClientBuilder.newBuilder()
            .register(JacksonObjectMapperProvider.class)
            .register(JacksonFeature.class)
            .build();
    }

}
