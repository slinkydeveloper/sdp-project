package com.slinkydeveloper.sdp.node.impl;

import com.google.protobuf.Empty;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.*;
import com.slinkydeveloper.sdp.node.acquisition.OverlappingSlidingWindowBuffer;
import com.slinkydeveloper.sdp.node.discovery.DiscoveryStateMachine;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.logging.Logger;

public class NodeServiceImpl extends NodeGrpc.NodeImplBase {

    private final static Logger LOG = LoggerConfig.getLogger(NodeServiceImpl.class);

    private final int myId;
    private final String myAddress;
    private final OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer;

    private final Object sensorReadingsTokenLock = new Object();
    private SensorsReadingsToken sensorReadingsToken;

    private final Object clientsLock = new Object();
    private Map<Integer, NodeGrpc.NodeBlockingStub> openClients;
    private List<Integer> nextNeighbours;

    private final Object discoveryStateMachineLock = new Object();
    private DiscoveryStateMachine discoveryStateMachine;

    public NodeServiceImpl(int myId, String myAddress, Map<Integer, String> initialKnownHosts, OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.slidingWindowBuffer = slidingWindowBuffer;

        this.endDiscoveryCallback(initialKnownHosts);
    }

    @Override
    public void passSensorsReadingsToken(final SensorsReadingsToken request, StreamObserver<Empty> responseObserver) {
        LOG.info("Received sensor readings token: " + request);

        // If we're discovering nodes, then keep the token on hold
        if (Utils.atomicExecuteOnPredicateSuccess(
                discoveryStateMachineLock,
                () -> this.discoveryStateMachine != null && this.discoveryStateMachine.isDiscovering(),
                () -> {
                    synchronized (sensorReadingsTokenLock) {
                        this.sensorReadingsToken = request;
                    }
                }
        )) {
            LOG.info("We're discovering, the token is on hold");
            responseObserver.onCompleted();
            return;
        }

        // Generate the new token to forward
        SensorsReadingsToken token = request;

        if (!request.containsLastMeasurements(this.myId)) {
            LOG.info("Token does not contain data from myself");
            Optional<Double> newAverage = slidingWindowBuffer.pollReducedMeasurement();
            if (newAverage.isPresent()) {
                token = token.toBuilder().putLastMeasurements(this.myId, newAverage.get()).build();
            }
        } else {
            LOG.info("Token already contains data from myself");
        }

        if (token.getLastMeasurementsMap().keySet().equals(this.getKnownNodes())) {
            LOG.info("We have data from everybody, I'm going to send values to the gateway");
            // TODO send data to gateway

            forwardSensorReadingsToken(SensorsReadingsToken.newBuilder().build());
        } else {
            forwardSensorReadingsToken(token);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void passDiscoveryToken(DiscoveryToken request, StreamObserver<Empty> responseObserver) {
        LOG.info("Received discovery token: " + request);

        // Generate the new token to forward
        DiscoveryToken token = null;
        synchronized (discoveryStateMachineLock) {
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
        synchronized (discoveryStateMachineLock) {
            this.discoveryStateMachine = null;
        }
        //TODO forward token on hold if any
    }

    private NodeGrpc.NodeBlockingStub getNextNeighbour(int index) {
        synchronized (clientsLock) {
            if (index >= this.nextNeighbours.size()) {
                return null;
            }
            return this.openClients.get(this.nextNeighbours.get(index));
        }
    }

    private Set<Integer> getKnownNodes() {
        Set<Integer> res = null;
        synchronized (clientsLock) {
            res = this.openClients.keySet();
        }
        res = new HashSet<>(res);
        res.add(this.myId);
        return res;
    }

    private void forwardSensorReadingsToken(SensorsReadingsToken token) {

    }

}
