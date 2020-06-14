package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.concurrent.AtomicFlag;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import com.slinkydeveloper.sdp.node.DiscoveryTokenType;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This class is thread safe
 */
public class DiscoveryHandler {

    private final static Logger LOG = LoggerConfig.getLogger(DiscoveryHandler.class);

    private final int myId;
    private final String myAddress;
    private final Consumer<Map<Integer, String>> temporaryKnownHostsCallback;
    private final Consumer<Map<Integer, String>> newKnownHostsCallback;
    private final Runnable endDiscoveryCallback;
    private final Consumer<Boolean> leaderCallbackAfterDiscoveredPropagated;

    private final AtomicFlag partecipating;

    public DiscoveryHandler(int myId, String myAddress, Consumer<Map<Integer, String>> temporaryKnownHostsCallback, Consumer<Map<Integer, String>> newKnownHostsCallback, Runnable endDiscoveryCallback, Consumer<Boolean> leaderCallbackAfterDiscoveredPropagated) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.temporaryKnownHostsCallback = temporaryKnownHostsCallback;
        this.newKnownHostsCallback = newKnownHostsCallback;
        this.endDiscoveryCallback = endDiscoveryCallback;
        this.leaderCallbackAfterDiscoveredPropagated = leaderCallbackAfterDiscoveredPropagated;

        this.partecipating = new AtomicFlag("Participating to discovery", false);
    }

    /**
     * Generate the token to start the discovery from this node
     */
    public DiscoveryToken startDiscovery(Map<Integer, String> previousKnownHosts, boolean generateNewSensorReadingsToken) {
        LOG.fine("Generating start discovery token");
        this.partecipating.setTrue();
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
     * @return the token to send to the next neighbour, if any
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
                this.partecipating.execute((old) -> {
                    this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                    return true;
                });
                return new AbstractMap.SimpleImmutableEntry<>(true, newTokenBuilder);
            } else if (token.getLeader() < this.myId) {
                // Keep the duplicated discovery if: the discovery token only if 'm already discovering or this token has new hosts we're unaware of
                // - We're not already discovering
                // - This discovery have new hosts we weren't aware of
                // - This discovery asks to generate a new SensorReadings token
                if (!this.isDiscovering() || hasNewHosts) {
                    this.partecipating.execute((old) -> {
                        this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                        return true;
                    });
                    return new AbstractMap.SimpleImmutableEntry<>(true, newTokenBuilder.setLeader(this.myId));
                } else {
                    LOG.fine("Discarding message because I'm already participating and the leader id is lower than mine");
                    this.partecipating.execute((old) -> {
                        this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                        return true;
                    });
                    return new AbstractMap.SimpleImmutableEntry<>(true, null);
                }
            } else {
                DiscoveryToken.Builder discoveredToken = newTokenBuilder
                    .setType(DiscoveryTokenType.DISCOVERED)
                    .clearPreviousKnownHosts();
                LOG.fine("Discovery phase completed and I'm the LEADER. Sending DISCOVERED token with nodes " + discoveredToken.getKnownHostsMap().keySet());

                // Notify the new hosts in the same lock of participating flag
                this.partecipating.execute((old) -> {
                    this.newKnownHostsCallback.accept(discoveredToken.getKnownHostsMap());
                    return false;
                });
                this.endDiscoveryCallback.run();
                return new AbstractMap.SimpleImmutableEntry<>(false, discoveredToken);
            }
        } else {
            if (token.getLeader() == this.myId) {
                LOG.fine("Discarding DISCOVERED token because I'm the LEADER");
                //TODO leader should notify to gateway the end of the discovery
                this.leaderCallbackAfterDiscoveredPropagated.accept(token.getGenerateNewSensorReadingsToken());
                return new AbstractMap.SimpleImmutableEntry<>(false, null);
            } else {
                this.partecipating.execute((old) -> {
                    this.newKnownHostsCallback.accept(token.getKnownHostsMap());
                    return false;
                });
                this.endDiscoveryCallback.run();
                return new AbstractMap.SimpleImmutableEntry<>(false, token.toBuilder());
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
        return this.partecipating.value();
    }

    public boolean executeIfIsDiscovering(Runnable runnable) {
        return this.partecipating.executeOnTrue(runnable);
    }

    public boolean executeIfNotDiscovering(Runnable runnable) {
        return this.partecipating.executeOnTrue(runnable);
    }

}
