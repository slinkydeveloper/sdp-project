package com.slinkydeveloper.sdp.client;

import com.slinkydeveloper.sdp.gateway.client.GatewayClientService;
import com.slinkydeveloper.sdp.gateway.client.impl.GatewayClientServiceImpl;
import com.slinkydeveloper.sdp.jersey.JerseyUtils;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataStatistics;

import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.stream.Collectors;

public class Client {

    public static void main(String[] args) {
        Console console = new Console();

        if (args.length != 1) {
            console.error("Missing gateway_host");
            console.print("Usage: sdp-client <gateway_host>");
            return;
        }

        URI gateway = URI.create(args[0]);
        GatewayClientService clientService = new GatewayClientServiceImpl(JerseyUtils.createClient(), gateway);

        console.header("SDP Gateway Client");
        console.header("Connected to " + gateway);

        console.listChoices(
            new SimpleImmutableEntry<>("Get connected nodes to the network", queryNodes(console, clientService)),
            new SimpleImmutableEntry<>("Get data statistics", queryStats(console, clientService)),
            new SimpleImmutableEntry<>("Listen to new events", listenEvents(console, clientService))
        );
    }

    public static Runnable queryNodes(Console console, GatewayClientService clientService) {
        return () -> {
            Set<Node> nodes;
            try {
                nodes = clientService.nodes();
            } catch (Exception e) {
                console.error("Error while invoking the gateway: " + e.getMessage());
                return;
            }

            console.print("Connected nodes to the system (please note, these information may be incorrect/not updated):");
            nodes.forEach(n -> console.print("Id " + n.getId() + ": " + n.getHost()));
        };
    }

    public static Runnable queryStats(Console console, GatewayClientService clientService) {
        return () -> {
            SensorDataStatistics stats;
            try {
                stats = clientService.data(console.numericInput("Insert the limit of the data statistics"));
            } catch (Exception e) {
                console.error("Error while invoking the gateway: " + e.getMessage());
                return;
            }

            console.print("Values in the stats");
            stats.getDataAverages().forEach(e ->
                console.print("Average '" + e.getValue().getAverage() + "' collected at '" + e.getKey() + "' with nodes '" + e.getValue().getParticipatingNodes() + "'")
            );
            console.newLine();
            console.print("Average: " + stats.getAverage());
            console.print("Standard deviation: " + stats.getStandardDeviation());
        };
    }

    public static Runnable listenEvents(Console console, GatewayClientService clientService) {
        return () -> {
            console.print("Press any button to stop listening");
            Runnable close = clientService.registerEventHandler(
                networkTopologyChangeEvent ->
                    console.print(
                        "Topology change\n  New topology " + networkTopologyChangeEvent.getNewNodes() +
                            "\n  Old topology " + networkTopologyChangeEvent.getOldNodes() +
                            "\n  Added nodes " + networkTopologyChangeEvent.getAddedNodes().stream().map(Node::getId).collect(Collectors.toSet()) +
                            "\n  Removed nodes " + networkTopologyChangeEvent.getRemovedNodes().stream().map(Node::getId).collect(Collectors.toSet())
                    ),
                newAverage ->
                    console.print(
                        "New sensor reading\n  Value: '" + newAverage.getAverage() +
                            "'\n  Participating nodes: " + newAverage.getParticipatingNodes()
                    )
            );

            System.console().readLine();
            close.run();
        };
    }

}
