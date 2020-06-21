package com.slinkydeveloper.sdp.node.impl;

import com.google.protobuf.Empty;
import com.slinkydeveloper.sdp.concurrent.AtomicPointer;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import com.slinkydeveloper.sdp.node.NewNeighbour;
import com.slinkydeveloper.sdp.node.NodeGrpc;
import com.slinkydeveloper.sdp.node.SensorReadingsToken;
import com.slinkydeveloper.sdp.node.acquisition.OverlappingSlidingWindowBuffer;
import com.slinkydeveloper.sdp.node.acquisition.SensorReadingsHandler;
import com.slinkydeveloper.sdp.node.network.DiscoveryHandler;
import com.slinkydeveloper.sdp.node.network.NodesRing;
import com.slinkydeveloper.sdp.timer.TimerScheduler;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.logging.Logger;

import static com.slinkydeveloper.sdp.SetUtils.minus;
import static com.slinkydeveloper.sdp.SetUtils.plus;

public class NodeServiceImpl extends NodeGrpc.NodeImplBase {

    private final static Logger LOG = LoggerConfig.getLogger(NodeServiceImpl.class);

    private final int myId;
    private final String myAddress;

    private final AtomicPointer<SensorReadingsToken> sensorReadingsTokenOnHold;
    private final NodesRing nodesRing;

    private final SensorReadingsHandler sensorReadingsHandler;
    private final DiscoveryHandler discoveryHandler;

    private final long waitMillis;

    private final TimerScheduler timerScheduler;

    public NodeServiceImpl(int myId, String myAddress, Map<Integer, String> initialKnownHosts, OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer) {
        this.myId = myId;
        this.myAddress = myAddress;

        this.sensorReadingsTokenOnHold = new AtomicPointer<>("Token on hold");
        this.nodesRing = new NodesRing(myId, myAddress, initialKnownHosts);

        this.sensorReadingsHandler = new SensorReadingsHandler(this.myId, slidingWindowBuffer);
        this.discoveryHandler = new DiscoveryHandler(
            this.myId,
            this.myAddress,
            this.nodesRing::insertNodes,
            this.nodesRing::setNodes,
            this::checkAndDispatchTokenOnHold,
            generateNewSensorReadingsToken -> {
                if (generateNewSensorReadingsToken && this.sensorReadingsTokenOnHold.isEmpty()) {
                    LOG.info("As a LEADER of the discovery, I'm going to regenerate the sensor readings token");
                    dispatchSensorReadingsToken(SensorReadingsToken.newBuilder().setGenerationUUID(UUID.randomUUID().toString()).build());
                }
            }
        );

        this.waitMillis = Optional.ofNullable(System.getenv("SDP_WAIT")).map(Long::parseLong).orElse(0l);

        this.timerScheduler = new TimerScheduler();
    }

    @Override
    public void passSensorReadingsToken(final SensorReadingsToken request, StreamObserver<Empty> responseObserver) {
        stopSensorReadingsTimeoutTimer();
        LOG.info("Received sensor readings token:\n" + request);

        // If we're discovering nodes, then keep the token on hold
        if (this.discoveryHandler.executeIfIsDiscovering(() -> this.sensorReadingsTokenOnHold.set(request))) {
            LOG.info("We're discovering, the token is on hold");
            reply(responseObserver);
            return;
        }

        // Generate the new token to forward
        SensorReadingsToken newToken = sensorReadingsHandler
            .handleSensorReadingsToken(request, this.nodesRing.getKnownHosts().keySet());

        // Reply to the client
        reply(responseObserver);

        if (newToken != null) {
            dispatchSensorReadingsToken(newToken);
        }
    }

    @Override
    public void passDiscoveryToken(DiscoveryToken request, StreamObserver<Empty> responseObserver) {
        stopDiscoveryTimeoutTimer();
        LOG.info("Received discovery token:\n" + request);

        // Generate the new token to forward
        Map<Integer, String> knownHosts = this.nodesRing.getKnownHosts();
        Map.Entry<Boolean, DiscoveryToken.Builder> token = this.discoveryHandler.handleReceivedDiscovery(
            request,
            minus(
                request.getKnownHostsMap().keySet(),
                plus(knownHosts.keySet(), this.myId)
            ).size() > 0,
            knownHosts
        );

        // Reply to the client
        reply(responseObserver);

        if (token.getValue() != null) {
            DiscoveryToken.Builder builder = token.getValue();
            if (!this.sensorReadingsTokenOnHold.isEmpty()) {
                builder.setGenerateNewSensorReadingsToken(false);
            }
            dispatchDiscoveryToken(builder.build(), token.getKey());
        } else if (token.getKey()) {
            startDiscoveryTimeoutTimer();
        }
    }

    @Override
    public void notifyNewNeighbour(NewNeighbour request, StreamObserver<Empty> responseObserver) {
        LOG.info("I have a new neighbour:\n" + request);

        // Temporary insert a new neighbour
        this.nodesRing.insertNode(request.getId(), request.getAddress());

        // Generate the discovery start token
        DiscoveryToken token = discoveryHandler
            .startDiscovery(this.nodesRing.getKnownHosts(), false);

        // Reply to the client
        reply(responseObserver);

        dispatchDiscoveryToken(token, true);
    }

    /**
     * After the service is started, we notify to the previous node my presence so it can start a new discovery
     * Then we're ready to receive the token
     */
    public void start() {
        NewNeighbour message = NewNeighbour
            .newBuilder()
            .setId(this.myId)
            .setAddress(this.myAddress)
            .build();

        int i = 0;
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> previous = nodesRing.getPrevious(i);
        while (previous != null) {
            waitBeforeDispatch();
            try {
                previous.getValue().notifyNewNeighbour(message);
                LOG.info("Notified my presence to the previous node in the ring (id " + previous.getKey() + ")");
                return;
            } catch (Exception e) {
                LOG.warning("Skipping " + (i + 1) + "° previous node (id " + previous.getKey() + ") because something wrong happened while passing the token: " + e);
                e.printStackTrace();
                i++;
                previous = this.nodesRing.getPrevious(i);
            }
        }
        LOG.info("I'm alone in the network");
        this.sensorReadingsTokenOnHold.set(SensorReadingsToken.newBuilder().setGenerationUUID(UUID.randomUUID().toString()).build());
    }

    /**
     * Start a new discovery (this operation is performed when the server is started)
     */
    private void startDiscoveryAfterFailure(Set<Integer> failedNodes, boolean askToGenerateNewSensorReadingsToken) {
        this.stopDiscoveryTimeoutTimer();

        LOG.warning("Something went wrong, trying to execute discovery again");

        // Generate starting token
        Map<Integer, String> previousKnownHostsMinusFailed = new HashMap<>(this.nodesRing.getKnownHosts());
        failedNodes.forEach(previousKnownHostsMinusFailed::remove);
        DiscoveryToken token = this.discoveryHandler.startDiscovery(previousKnownHostsMinusFailed, askToGenerateNewSensorReadingsToken);

        // Dispatch that token
        dispatchDiscoveryToken(token, true);
    }

    private void checkAndDispatchTokenOnHold() {
        // If there is a sensor readings token on hold, then forward it
        SensorReadingsToken token = this.sensorReadingsTokenOnHold.getAndClear();
        if (token != null) {
            LOG.info("Discovery ended and I have the sensor readings token, forwarding:\n" + token);
            dispatchSensorReadingsToken(token);
        }
    }

    private void dispatchSensorReadingsToken(SensorReadingsToken token) {
        // Forward to next neighbour
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> nextNeighbour = nodesRing.getNext(0);
        if (nextNeighbour != null) {
            waitBeforeDispatch();
            try {
                nextNeighbour.getValue().passSensorReadingsToken(token);
                LOG.info("Sensors reading token passed successfully to next neighbour " + nextNeighbour.getKey());
                this.startSensorReadingsTimeoutTimer();
                this.sensorReadingsTokenOnHold.clear();
            } catch (Exception e) {
                LOG.warning("Failure while trying to pass the sensors readings token to neighbour " + nextNeighbour.getKey() + ": " + e);
                e.printStackTrace();
                this.sensorReadingsTokenOnHold.set(token);
                startDiscoveryAfterFailure(Collections.singleton(nextNeighbour.getKey()), false);
            }
        } else {
            throw new IllegalStateException("All the neighbours are unavailable!");
        }
    }

    private void dispatchDiscoveryToken(DiscoveryToken token, boolean startTimer) {
        // Forward to next neighbour and just skip failing ones
        int i = 0;
        Map.Entry<Integer, NodeGrpc.NodeBlockingStub> nextNeighbour = this.nodesRing.getNext(0);
        while (nextNeighbour != null) {
            waitBeforeDispatch();
            try {
                nextNeighbour.getValue().passDiscoveryToken(token);
                LOG.info("Discovery token passed successfully to " + (i + 1) + "° neighbour (id " + nextNeighbour.getKey() + ")");
                if (startTimer) {
                    startDiscoveryTimeoutTimer();
                }
                return;
            } catch (Exception e) {
                LOG.warning("Skipping " + (i + 1) + "° neighbour (id " + nextNeighbour.getKey() + ") because something wrong happened while passing the token: " + e);

                token = this.discoveryHandler.fixTokenWhenHostIsUnavailable(token, nextNeighbour.getKey());
                i++;
                nextNeighbour = this.nodesRing.getNext(i);
            }
        }
        this.nodesRing.setNodes(Collections.emptyMap());
        throw new IllegalStateException("All the neighbours are unavailable! Looks like I'm alone!");
    }

    private void reply(StreamObserver<Empty> emptyStream) {
        emptyStream.onNext(Empty.newBuilder().build());
        emptyStream.onCompleted();
    }

    private void waitBeforeDispatch() {
        try {
            Thread.sleep(this.waitMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startDiscoveryTimeoutTimer() {
        stopSensorReadingsTimeoutTimer();
        long timeout = computeTimeout(1);
        this.timerScheduler.schedule(
            "discovery-timeout",
            timeout,
            () -> this.startDiscoveryAfterFailure(Collections.emptySet(), false)
        );
    }

    private void stopDiscoveryTimeoutTimer() {
        this.timerScheduler.cancel("discovery-timeout");
    }

    private void startSensorReadingsTimeoutTimer() {
        long timeout = computeTimeout(1);
        this.timerScheduler.conditionalSchedule(
            s -> !s.contains("discovery-timeout"),
            "sensor-readings-timeout",
            timeout,
            () -> this.startDiscoveryAfterFailure(Collections.emptySet(), true)
        );
    }

    private void stopSensorReadingsTimeoutTimer() {
        this.timerScheduler.cancel("sensor-readings-timeout");
    }

    private long computeTimeout(long factor) {
        int size = this.nodesRing.getKnownHosts().size();
        if (size == 0) {
            return Long.MAX_VALUE;
        }
        return size * 10 * 1000 * factor;
    }

}
