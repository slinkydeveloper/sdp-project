package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.node.NodeGrpc;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Utils {

    public static NodeGrpc.NodeBlockingStub buildNewClient(String address) {
        return NodeGrpc.newBlockingStub(
            ManagedChannelBuilder
                .forTarget(address)
                .usePlaintext()
                .build()
        );
    }

    public static List<Integer> generateNextNeighboursList(Set<Integer> neighbours, int myId) {
        List<Integer> nextNeighbours = new ArrayList<>(neighbours);
        if (!nextNeighbours.contains(myId)) {
            nextNeighbours.add(myId);
        }
        Collections.sort(nextNeighbours);

        int myIndex = nextNeighbours.indexOf(myId);

        Collections.rotate(nextNeighbours, nextNeighbours.size() - myIndex);
        nextNeighbours.remove(0);

        return Collections.unmodifiableList(nextNeighbours);
    }

}
