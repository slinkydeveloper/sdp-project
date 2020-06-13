package com.slinkydeveloper.sdp.node.impl;

import com.google.protobuf.Empty;
import com.slinkydeveloper.sdp.concurrent.AtomicPointer;
import com.slinkydeveloper.sdp.concurrent.AtomicUtils;
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

    private final AtomicPointer<SensorsReadingsToken> sensorReadingsToken;

    private final Object clientsLock = new Object();
    private Map<Integer, NodeGrpc.NodeBlockingStub> openClients;
    private List<Integer> nextNeighbours;

    private final Object discoveryStateMachineLock = new Object();
    private final DiscoveryStateMachine discoveryStateMachine;

    public NodeServiceImpl(int myId, String myAddress, Map<Integer, String> initialKnownHosts, OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.slidingWindowBuffer = slidingWindowBuffer;

        this.sensorReadingsToken = new AtomicPointer<>("TokenInThisNode");
        this.configureKnownHosts(initialKnownHosts);

        this.discoveryStateMachine = new DiscoveryStateMachine(this.myId, this.myAddress, this::endDiscoveryCallback);
    }

    @Override
    public void passSensorsReadingsToken(final SensorsReadingsToken request, StreamObserver<Empty> responseObserver) {
        LOG.info("Received sensor readings token:\n" + request);

        // If we're discovering nodes, then keep the token on hold
        if (AtomicUtils.atomicCheckPredicate(discoveryStateMachineLock, this.discoveryStateMachine::isDiscovering)) {
            LOG.info("We're discovering, the token is on hold");
            this.sensorReadingsToken.set(request);
            reply(responseObserver);
            return;
        }

        // Generate the new token to forward
        SensorsReadingsToken token = request;

        if (!request.containsLastMeasurements(this.myId)) {
            LOG.fine("Token does not contain data from myself");
            Optional<Double> newAverage = slidingWindowBuffer.pollReducedMeasurement();
            LOG.fine("New average: " + newAverage);
            if (newAverage.isPresent()) {
                token = token.toBuilder().putLastMeasurements(this.myId, newAverage.get()).build();
            }
        } else {
            LOG.fine("Token already contains data from myself");
        }
        this.sensorReadingsToken.set(token);

        reply(responseObserver);

        if (token.getLastMeasurementsMap().keySet().containsAll(this.getKnownNodes())) {
            LOG.info("We have data from everybody, I'm going to send values to the gateway");
            // TODO send data to gateway

            forwardSensorReadingsToken(SensorsReadingsToken.newBuilder().build());
        } else {
            forwardSensorReadingsToken(token);
        }
    }

    @Override
    public void passDiscoveryToken(DiscoveryToken request, StreamObserver<Empty> responseObserver) {
        LOG.info("Received discovery token:\n" + request);

        // Generate the new token to forward
        DiscoveryToken token = AtomicUtils
            .atomicGet(discoveryStateMachineLock, () -> this.discoveryStateMachine.onReceivedDiscovery(request));

        // Reply to the client
        reply(responseObserver);

        if (token != null) {
            forwardDiscoveryToken(token);
        }
    }

    @Override
    public void notifyNewNeighbour(NewNeighbour request, StreamObserver<Empty> responseObserver) {
        LOG.info("I have a new neighbour:\n" + request);
        DiscoveryToken token = null;
        synchronized (discoveryStateMachineLock) {
            synchronized (clientsLock) {
                this.openClients.put(
                    request.getId(),
                    Utils.buildNewClient(request.getAddress())
                );

                // If for some reason the next neighbour is wrong, we need to reorganize the list
                Set<Integer> newNeighbours = new HashSet<>(this.nextNeighbours);
                newNeighbours.add(request.getId());
                this.nextNeighbours = Utils.generateNextNeighboursList(newNeighbours, this.myId);
                LOG.fine("Temporary new nextNeighbours: " + this.nextNeighbours);
            }

            // Generate starting token if a discovery is not available
            //TODO what happens if we're already running the service discovery?
            token = discoveryStateMachine.startDiscovery();
        }

        reply(responseObserver);

        if (token != null) {
            forwardDiscoveryToken(token);
        }
    }

    @Override
    public void doYouHaveTheToken(SearchToken request, StreamObserver<Empty> responseObserver) {
        responseObserver.onError(new IllegalStateException("Method doYouHaveTheToken not implemented"));
    }

    /**
     * After the service is started, we notify to the previous node my presence so it can start a new discovery
     * Then we're ready to receive the token
     */
    public void start() {
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> previous = this.getPreviousNeighbour();
        if (previous != null) {
            LOG.info("Notifying my presence to the previous node in the ring (id " + previous.getKey() + ")");
            //TODO What if this node is not reachable?
            previous.getValue().notifyNewNeighbour(
                NewNeighbour
                    .newBuilder()
                    .setId(this.myId)
                    .setAddress(this.myAddress)
                    .build()
            );
        } else {
            LOG.info("I'm alone in the network");
            this.sensorReadingsToken.set(SensorsReadingsToken.newBuilder().build());
        }
    }

    /**
     * Start a new discovery (this operation is performed when the server is started)
     */
    private void startDiscoveryAfterFailure() {
        LOG.warning("Something went wrong, trying to execute discovery again");

        // Generate starting token
        DiscoveryToken token = AtomicUtils
            .atomicGet(discoveryStateMachineLock, this.discoveryStateMachine::startDiscovery);

        forwardDiscoveryToken(token);
    }

    private void configureKnownHosts(Map<Integer, String> knownHosts) {
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
    }

    private void endDiscoveryCallback(Map<Integer, String> knownHosts) {
        LOG.fine("Registering new hosts map: " + knownHosts);

        this.configureKnownHosts(knownHosts);

        // If there is a sensor readings token on hold, then forward it
        SensorsReadingsToken token = this.sensorReadingsToken.value();
        if (token != null) {
            LOG.info("Discovery ended and I have the sensor readings token, forwarding:\n" + token);
            forwardSensorReadingsToken(token);
        }
    }

    private Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getNextNeighbour(int index) {
        synchronized (clientsLock) {
            if (index >= this.nextNeighbours.size()) {
                return null;
            }
            int id = this.nextNeighbours.get(index);
            return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
        }
    }

    private Map.Entry<Integer, NodeGrpc.NodeBlockingStub> getPreviousNeighbour() {
        synchronized (clientsLock) {
            if (this.openClients.isEmpty()) {
                return null;
            }
            int id = this.nextNeighbours.get(this.nextNeighbours.size() - 1);
            return new AbstractMap.SimpleImmutableEntry<>(id, this.openClients.get(id));
        }
    }

    private Set<Integer> getKnownNodes() {
        Set<Integer> res = AtomicUtils.atomicGet(clientsLock, this.openClients::keySet);
        res = new HashSet<>(res);
        res.add(this.myId);
        return res;
    }

    private void forwardSensorReadingsToken(SensorsReadingsToken token) {
        // Forward to next neighbour
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> nextNeighbour = getNextNeighbour(0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (nextNeighbour != null) {
            try {
                nextNeighbour.getValue().passSensorsReadingsToken(token);
                LOG.info("Sensors reading token passed successfully to next neighbour " + nextNeighbour.getKey());
                this.sensorReadingsToken.clear();
            } catch (Exception e) {
                LOG.warning("Failure while trying to pass the sensors readings token to neighbour " + +nextNeighbour.getKey() + ": " + e);
                e.printStackTrace();
                startDiscoveryAfterFailure();
            }
        } else {
            throw new IllegalStateException("All the neighbours are unavailable!");
        }
    }

    private void forwardDiscoveryToken(DiscoveryToken token) {
        // Forward to next neighbour and just skip failing ones
        int i = 0;
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> nextNeighbour = getNextNeighbour(0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (nextNeighbour != null) {
            try {
                nextNeighbour.getValue().passDiscoveryToken(token);
                LOG.info("Discovery token passed successfully to " + (i + 1) + "° neighbour (id " + nextNeighbour.getKey() + ")");
                return;
            } catch (Exception e) {
                LOG.warning("Skipping " + (i + 1) + "° neighbour (id " + nextNeighbour.getKey() + ") because something wrong happened while passing the token: " + e);
                i++;
                nextNeighbour = getNextNeighbour(i);
            }
        }
        configureKnownHosts(Collections.emptyMap());
        throw new IllegalStateException("All the neighbours are unavailable! Looks like I'm alone!");
    }

    private void reply(StreamObserver<Empty> emptyStream) {
        emptyStream.onNext(Empty.newBuilder().build());
        emptyStream.onCompleted();
    }

}
