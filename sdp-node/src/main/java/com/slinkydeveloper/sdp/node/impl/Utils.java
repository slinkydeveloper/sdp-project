package com.slinkydeveloper.sdp.node.impl;

import com.slinkydeveloper.sdp.node.NodeGrpc;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Utils {

    static NodeGrpc.NodeBlockingStub buildNewClient(String address) {
        return NodeGrpc.newBlockingStub(ManagedChannelBuilder.forTarget(address).build());
    }

    static List<Integer> generateNextNeighboursList(Set<Integer> neighbours, int myId) {
        List<Integer> nextNeighbours = new ArrayList<>(neighbours);
        if (!nextNeighbours.contains(myId)) {
            nextNeighbours.add(myId);
        }
        Collections.sort(nextNeighbours);

        int myIndex = nextNeighbours.indexOf(myId);

        nextNeighbours.remove(Integer.valueOf(myId));
        Collections.rotate(nextNeighbours, myIndex);

        return nextNeighbours;
    }

    static boolean atomicExecuteOnPredicate(Object lock, Supplier<Boolean> predicate, Consumer<Boolean> executeInLockedSection) {
        boolean result;
        synchronized (lock) {
            result = predicate.get();
            executeInLockedSection.accept(result);
        }
        return result;
    }

    static boolean atomicExecuteOnPredicateSuccess(Object lock, Supplier<Boolean> predicate, Runnable executeInLockedSection) {
        return atomicExecuteOnPredicate(lock, predicate, res -> {
            if (res) {
                executeInLockedSection.run();
            }
        });
    }
}
