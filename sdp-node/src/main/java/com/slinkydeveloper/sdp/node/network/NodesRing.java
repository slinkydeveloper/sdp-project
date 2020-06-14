package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.NodeGrpc;

import java.util.*;
import java.util.logging.Logger;

public class NodesRing {

    private final static Logger LOG = LoggerConfig.getLogger(NodesRing.class);

    private final int myId;
    private final String myAddress;

    private Map<Integer, NodeGrpc.NodeBlockingStub> openClients;
    private List<Integer> nextNeighbours;

    public NodesRing(int myId, String myAddress, Map<Integer, String> initialKnownHosts) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.configureKnownHosts(initialKnownHosts);
    }

    public synchronized void insertNewNeighbour(int id, String address) {
        this.openClients.put(
            id,
            Utils.buildNewClient(address)
        );

        Set<Integer> newNeighbours = new HashSet<>(this.nextNeighbours);
        newNeighbours.add(id);
        this.nextNeighbours = Utils.generateNextNeighboursList(newNeighbours, this.myId);

        LOG.fine("New next neighbours: " + this.nextNeighbours);
    }

    public synchronized void configureKnownHosts(Map<Integer, String> knownHosts) {
        Map<Integer, NodeGrpc.NodeBlockingStub> newOpenClients = new HashMap<>();

        knownHosts.forEach((id, address) -> {
            if (id != this.myId) {
                NodeGrpc.NodeBlockingStub stub = Optional
                    .ofNullable(this.openClients)
                    .map(m -> m.get(id))
                    .orElse(Utils.buildNewClient(address));

                newOpenClients.put(id, stub);
            }
        });

        this.openClients = newOpenClients;
        this.nextNeighbours = Utils.generateNextNeighboursList(knownHosts.keySet(), this.myId);

        LOG.fine("New next neighbours: " + this.nextNeighbours);
    }

    public synchronized Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getNext(int index) {
        if (index >= this.nextNeighbours.size()) {
            return null;
        }
        int id = this.nextNeighbours.get(index);
        return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
    }

    public synchronized Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getPrevious() {
        if (this.openClients.isEmpty()) {
            return null;
        }
        int id = this.nextNeighbours.get(this.nextNeighbours.size() - 1);
        return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
    }

    public Set<Integer> getKnownNodes() {
        Set<Integer> res;
        synchronized (this) {
            res = this.openClients.keySet();
        }
        res = new HashSet<>(res);
        res.add(this.myId);
        return res;
    }

}
