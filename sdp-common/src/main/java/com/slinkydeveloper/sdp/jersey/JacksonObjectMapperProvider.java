package com.slinkydeveloper.sdp.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper defaultObjectMapper;

    public JacksonObjectMapperProvider() {
        defaultObjectMapper = new ObjectMapper();
        defaultObjectMapper
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return this.defaultObjectMapper;
    }
}
