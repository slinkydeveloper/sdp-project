package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.NodeGrpc;

import java.util.*;
import java.util.logging.Logger;

public class NodesRing {

    private final static Logger LOG = LoggerConfig.getLogger(NodesRing.class);

    private final int myId;
    private final String myAddress;

    private Map<Integer, String> knownHosts;
    private Map<Integer, NodeGrpc.NodeBlockingStub> openClients;
    private List<Integer> nextNeighbours;

    public NodesRing(int myId, String myAddress, Map<Integer, String> initialKnownHosts) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.setNodes(initialKnownHosts);
    }

    public synchronized void insertNode(int id, String address) {
        this.knownHosts.put(id, address);
        if (!this.openClients.containsKey(id) && id != this.myId) {
            this.openClients.put(
                id,
                Utils.buildNewClient(address)
            );

            Set<Integer> newNeighbours = new HashSet<>(this.nextNeighbours);
            newNeighbours.add(id);
            this.nextNeighbours = Utils.generateNextNeighboursList(newNeighbours, this.myId);

            LOG.fine("New next neighbours: " + this.nextNeighbours);
        }
    }


    public synchronized void insertNodes(Map<Integer, String> newNeighbours) {
        newNeighbours.forEach(this::insertNode);
    }
    public synchronized void setNodes(Map<Integer, String> knownHosts) {
        this.knownHosts = new HashMap<>(knownHosts);
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

    public synchronized Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getNext(int skip) {
        if (skip >= this.nextNeighbours.size()) {
            return null;
        }
        int id = this.nextNeighbours.get(skip);
        return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
    }

    public synchronized Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getPrevious(int skip) {
        if (this.openClients.isEmpty() || this.nextNeighbours.size() - 1 - skip < 0) {
            return null;
        }
        int id = this.nextNeighbours.get(this.nextNeighbours.size() - 1 - skip);
        return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
    }

    public Map<Integer, String> getKnownHosts() {
        return Collections.unmodifiableMap(this.knownHosts);
    }

}
