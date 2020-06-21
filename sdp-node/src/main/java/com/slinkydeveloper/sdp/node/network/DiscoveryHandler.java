package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.concurrent.AtomicPointer;
import com.slinkydeveloper.sdp.gateway.GatewayService;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import com.slinkydeveloper.sdp.node.DiscoveryTokenType;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.slinkydeveloper.sdp.node.network.DiscoveryStatus.*;

/**
 * This class is thread safe
 */
public class DiscoveryHandler {

    private final static Logger LOG = LoggerConfig.getLogger(DiscoveryHandler.class);

    private final int myId;
    private final String myAddress;
    private final GatewayService gatewayService;
    private final Consumer<Map<Integer, String>> temporaryKnownHostsCallback;
    private final Consumer<Map<Integer, String>> newKnownHostsCallback;
    private final Runnable endDiscoveryCallback;
    private final Consumer<Boolean> leaderCallbackAfterDiscoveredPropagated;


    private final AtomicPointer<DiscoveryStatus> status;

    public DiscoveryHandler(int myId, String myAddress, GatewayService gatewayService, Consumer<Map<Integer, String>> temporaryKnownHostsCallback, Consumer<Map<Integer, String>> newKnownHostsCallback, Runnable endDiscoveryCallback, Consumer<Boolean> leaderCallbackAfterDiscoveredPropagated) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.gatewayService = gatewayService;
        this.temporaryKnownHostsCallback = temporaryKnownHostsCallback;
        this.newKnownHostsCallback = newKnownHostsCallback;
        this.endDiscoveryCallback = endDiscoveryCallback;
        this.leaderCallbackAfterDiscoveredPropagated = leaderCallbackAfterDiscoveredPropagated;

        this.status = new AtomicPointer<>("Participating to discovery", DiscoveryStatus.NOT_PARTICIPATING);
    }

    /**
     * Generate the token to start the discovery from this node
     */
    public DiscoveryToken startDiscovery(Map<Integer, String> previousKnownHosts, boolean generateNewSensorReadingsToken) {
        LOG.fine("Generating start discovery token");
        this.status.set(generateNewSensorReadingsToken ? GENERATE_TOKEN_PARTICIPATION : NORMAL_PARTICIPATION);
        return DiscoveryToken.newBuilder()
            .setType(DiscoveryTokenType.DISCOVERY)
            .setLeader(this.myId)
            .putAllPreviousKnownHosts(previousKnownHosts)
            .putPreviousKnownHosts(this.myId, this.myAddress)
            .putKnownHosts(this.myId, this.myAddress)
            .setGenerateNewSensorReadingsToken(generateNewSensorReadingsToken)
            .build();
    }

    /**
     * Compute next token to send to the next neighbour
     *
     * @param token received token
     * @return a true key if we're expecting new discovery tokens and the token to send to the next neighbour, if any
     */
    public Map.Entry<Boolean, DiscoveryToken.Builder> handleReceivedDiscovery(DiscoveryToken token, boolean hasNewHosts, Map<Integer, String> previousKnownHosts) {
        // Algorithm implemented as https://en.wikipedia.org/wiki/Chang_and_Roberts_algorithm#The_algorithm
        // but tweaked to do the service discovery
        if (token.getType() == DiscoveryTokenType.DISCOVERY) {
            // Add myself in the new token
            DiscoveryToken.Builder newTokenBuilder = token
                .toBuilder()
                .putKnownHosts(this.myId, this.myAddress)
                .putAllPreviousKnownHosts(previousKnownHosts);

            if (token.getLeader() > this.myId) {
                this.status.swap(old -> {
                    this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                    return token.getGenerateNewSensorReadingsToken() ? GENERATE_TOKEN_PARTICIPATION : NORMAL_PARTICIPATION;
                });
                return new SimpleImmutableEntry<>(true, newTokenBuilder);
            } else if (token.getLeader() < this.myId) {
                // Keep the duplicated discovery if: the discovery token only if 'm already discovering or this token has new hosts we're unaware of
                // - We're not already discovering
                // - This discovery have new hosts we weren't aware of
                // - This discovery asks to generate a new SensorReadings token but we're participating to a normal discovery
                DiscoveryStatus status = this.status.get();
                boolean shouldForward =
                    !status.isParticipating() ||
                        hasNewHosts ||
                        token.getGenerateNewSensorReadingsToken() && status == NORMAL_PARTICIPATION;

                this.status.swap(old -> {
                    this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                    return token.getGenerateNewSensorReadingsToken() ? GENERATE_TOKEN_PARTICIPATION : NORMAL_PARTICIPATION;
                });

                LOG.fine("Debug condition token.getLeader() < this.myId:\n" +
                    "!status.isParticipating() = " + !status.isParticipating() + "\n" +
                    "hasNewHosts = " + hasNewHosts + "\n" +
                    "(token.getGenerateNewSensorReadingsToken() && status == NORMAL_PARTICIPATION) = " + (token.getGenerateNewSensorReadingsToken() && status == NORMAL_PARTICIPATION));

                return new SimpleImmutableEntry<>(true, shouldForward ? newTokenBuilder.setLeader(this.myId) : null);
            } else if (status.get() == GENERATE_TOKEN_PARTICIPATION && !token.getGenerateNewSensorReadingsToken()) {
                LOG.fine("I'm the leader, but I'm gonna discard this discovery because I was expecting a token with getGenerateNewSensorReadingsToken() = true");
                return new SimpleImmutableEntry<>(true, null);
            } else {
                DiscoveryToken.Builder discoveredToken = newTokenBuilder
                    .setType(DiscoveryTokenType.DISCOVERED)
                    .clearPreviousKnownHosts();
                LOG.fine("Discovery phase completed and I'm the LEADER. Sending DISCOVERED token with nodes " + discoveredToken.getKnownHostsMap().keySet());

                // Notify the new hosts in the same lock of participating flag
                this.status.swap((old) -> {
                    this.newKnownHostsCallback.accept(discoveredToken.getKnownHostsMap());
                    return NOT_PARTICIPATING;
                });
                this.endDiscoveryCallback.run();
                return new SimpleImmutableEntry<>(false, discoveredToken);
            }
        } else {
            if (token.getLeader() == this.myId) {
                LOG.fine("Discarding DISCOVERED token because I'm the LEADER and the discovery is finished");

                this.gatewayService.publishNewHosts(this.myId, token.getKnownHostsMap());

                this.leaderCallbackAfterDiscoveredPropagated.accept(token.getGenerateNewSensorReadingsToken());
                return new SimpleImmutableEntry<>(false, null);
            } else {
                this.status.swap((old) -> {
                    this.newKnownHostsCallback.accept(token.getKnownHostsMap());
                    return NOT_PARTICIPATING;
                });
                this.endDiscoveryCallback.run();
                return new SimpleImmutableEntry<>(false, token.toBuilder());
            }
        }
    }

    public DiscoveryToken fixTokenWhenHostIsUnavailable(DiscoveryToken token, int unavailableNode) {
        DiscoveryToken.Builder builder = token.toBuilder()
            .removePreviousKnownHosts(unavailableNode)
            .removeKnownHosts(unavailableNode);

        if (token.getLeader() == unavailableNode) {
            builder.setLeader(this.myId);
        }

        return builder.build();
    }

    public boolean isDiscovering() {
        return this.status.get().isParticipating();
    }

    public boolean executeIfIsDiscovering(Runnable runnable) {
        return this.status.map(s -> {
            if (s.isParticipating()) {
                runnable.run();
                return true;
            }
            return false;
        });
    }

}
