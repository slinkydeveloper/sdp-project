package com.slinkydeveloper.sdp.node.impl;

import com.google.protobuf.Empty;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.*;
import com.slinkydeveloper.sdp.node.discovery.DiscoveryStateMachine;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.logging.Logger;

public class NodeServiceImpl extends NodeGrpc.NodeImplBase {

    private final static Logger LOG = LoggerConfig.getLogger(NodeServiceImpl.class);

    private final int myId;
    private final String myAddress;

    private final Object clientsLock = new Object();
    private Map<Integer, NodeGrpc.NodeBlockingStub> openClients;
    private List<Integer> nextNeighbours;

    private final Object discoverStateMachineLock = new Object();
    private DiscoveryStateMachine discoveryStateMachine;

    public NodeServiceImpl(int myId, String myAddress, Map<Integer, String> initialKnownHosts) {
        this.myId = myId;
        this.myAddress = myAddress;

        this.endDiscoveryCallback(initialKnownHosts);
    }

    @Override
    public void passSensorsReadingsToken(SensorsReadingsToken request, StreamObserver<Empty> responseObserver) {
        responseObserver.onError(new IllegalStateException("Method passSensorsReadingsToken not implemented"));
    }

    @Override
    public void passDiscoveryToken(DiscoveryToken request, StreamObserver<Empty> responseObserver) {
        LOG.info("Received discovery token: " + request);

        // Generate the new token to forward
        DiscoveryToken token = null;
        synchronized (discoverStateMachineLock) {
            if (discoveryStateMachine == null) {
                discoveryStateMachine = new DiscoveryStateMachine(this.myId, this.myAddress, this::endDiscoveryCallback);
            }
            token = discoveryStateMachine.onReceivedDiscovery(request);
        }

        // Reply to the client
        responseObserver.onCompleted();

        // Forward to next neighbour and just skip failing ones
        int i = 0;
        NodeGrpc.NodeBlockingStub nextNeighbour = getNextNeighbour(0);
        while (nextNeighbour != null) {
            try {
                nextNeighbour.passDiscoveryToken(token);
                LOG.info("Discovery token passed successfully to neighbour " + i + ": " + token);
                return;
            } catch (Exception e) {
                LOG.warning("Skipping neighbour with index " + i + " because something wrong happened while passing the token: " + e);
                i++;
                nextNeighbour = getNextNeighbour(i);
            }
        }

    }

    @Override
    public void notifyNewNeighbour(NewNeighbour request, StreamObserver<Empty> responseObserver) {
        responseObserver.onError(new IllegalStateException("Method notifyNewNeighbour not implemented"));
    }

    @Override
    public void doYouHaveTheToken(SearchToken request, StreamObserver<Empty> responseObserver) {
        responseObserver.onError(new IllegalStateException("Method doYouHaveTheToken not implemented"));
    }

    private void endDiscoveryCallback(Map<Integer, String> knownHosts) {
        LOG.fine("Registering new hosts map: " + knownHosts);
        synchronized (clientsLock) {
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
        synchronized (discoverStateMachineLock) {
            this.discoveryStateMachine = null;
        }
    }

    private NodeGrpc.NodeBlockingStub getNextNeighbour(int index) {
        synchronized (clientsLock) {
            if (index >= this.nextNeighbours.size()) {
                return null;
            }
            return this.openClients.get(this.nextNeighbours.get(index));
        }
    }

}
