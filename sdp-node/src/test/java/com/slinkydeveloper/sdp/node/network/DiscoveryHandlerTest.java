package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.gateway.client.impl.GatewayNodeServiceFileLogger;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryHandlerTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    public void fourNodesDiscovery(int startingNode) {
        GatewayNodeServiceFileLogger service = new GatewayNodeServiceFileLogger("sample.txt");

        Consumer<Map<Integer, String>> assertCorrectDiscovery = m -> assertThat(m)
            .containsOnly(
                new AbstractMap.SimpleImmutableEntry<>(1, "localhost:8080"),
                new AbstractMap.SimpleImmutableEntry<>(2, "localhost:8081"),
                new AbstractMap.SimpleImmutableEntry<>(3, "localhost:8082"),
                new AbstractMap.SimpleImmutableEntry<>(4, "localhost:8083")
            );

        List<DiscoveryHandler> nodes = Arrays.asList(
            new DiscoveryHandler(1, "localhost:8080", service, m -> {
            }, assertCorrectDiscovery, () -> {
            }, m -> {
            }),
            new DiscoveryHandler(2, "localhost:8081", service, m -> {
            }, assertCorrectDiscovery, () -> {
            }, m -> {
            }),
            new DiscoveryHandler(3, "localhost:8082", service, m -> {
            }, assertCorrectDiscovery, () -> {
            }, m -> {
            }),
            new DiscoveryHandler(4, "localhost:8083", service, m -> {
            }, assertCorrectDiscovery, () -> {
            }, m -> {
            })
        );

        DiscoveryToken token = nodes.get(startingNode).startDiscovery(Collections.emptyMap(), false);
        assertThat(token)
            .isNotNull();

        int i = (startingNode + 1) % 4;
        while (token != null) {
            token = Optional.ofNullable(
                nodes.get(i).handleReceivedDiscovery(token, false, Collections.emptyMap()).getValue()
            ).map(DiscoveryToken.Builder::build).orElse(null);
            i = (i + 1) % 4;
        }
    }

}
