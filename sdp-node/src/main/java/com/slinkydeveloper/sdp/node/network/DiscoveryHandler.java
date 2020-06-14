package com.slinkydeveloper.sdp.node.network;

import com.slinkydeveloper.sdp.concurrent.AtomicFlag;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import com.slinkydeveloper.sdp.node.DiscoveryTokenType;

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

    private final AtomicFlag partecipating;

    public DiscoveryHandler(int myId, String myAddress, Consumer<Map<Integer, String>> temporaryKnownHostsCallback, Consumer<Map<Integer, String>> newKnownHostsCallback, Runnable endDiscoveryCallback) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.temporaryKnownHostsCallback = temporaryKnownHostsCallback;
        this.newKnownHostsCallback = newKnownHostsCallback;
        this.endDiscoveryCallback = endDiscoveryCallback;

        this.partecipating = new AtomicFlag("Participating to discovery", false);
    }

    /**
     * Generate the token to start the discovery from this node
     */
    public DiscoveryToken startDiscovery(Map<Integer, String> previousKnownHosts) {
        LOG.fine("Generating start discovery token");
        this.partecipating.setTrue();
        return DiscoveryToken.newBuilder()
            .setType(DiscoveryTokenType.DISCOVERY)
            .setLeader(this.myId)
            .putAllPreviousKnownHosts(previousKnownHosts)
            .putPreviousKnownHosts(this.myId, this.myAddress)
            .putKnownHosts(this.myId, this.myAddress)
            .build();
    }

    /**
     * Compute next token to send to the next neighbour
     *
     * @param token received token
     * @return the token to send to the next neighbour, if any
     */
    public DiscoveryToken handleReceivedDiscovery(DiscoveryToken token) {
        LOG.fine("New message: \n" + token.toString());
        // Algorithm implemented as https://en.wikipedia.org/wiki/Chang_and_Roberts_algorithm#The_algorithm
        // but tweaked to do the service discovery
        if (token.getType() == DiscoveryTokenType.DISCOVERY) {
            // Add myself in the new token
            DiscoveryToken.Builder newTokenBuilder = token
                .toBuilder()
                .putKnownHosts(this.myId, this.myAddress);

            if (token.getLeader() > this.myId) {
                this.partecipating.execute((old) -> {
                    this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                    return true;
                });
                return newTokenBuilder.build();
            } else if (token.getLeader() < this.myId) {
                if (this.partecipating.value()) {
                    LOG.fine("Discarding message because I'm already partecipating and the leader id is lower than mine");
                    return null;
                } else {
                    this.partecipating.execute((old) -> {
                        this.temporaryKnownHostsCallback.accept(token.getPreviousKnownHostsMap());
                        return true;
                    });
                    return newTokenBuilder.setLeader(this.myId).build();
                }
            } else {
                LOG.fine("Discovery phase completed. Notifying results to the network");

                // Notify the new hosts in the same lock of partecipating flag
                this.partecipating.execute((old) -> {
                    this.newKnownHostsCallback.accept(token.getKnownHostsMap());
                    return false;
                });
                this.endDiscoveryCallback.run();
                return newTokenBuilder
                    .setType(DiscoveryTokenType.DISCOVERED)
                    .build();
            }
        } else {
            if (token.getLeader() == this.myId) {
                LOG.fine("Discarding message because the discovery is finished");
                return null;
            } else {
                this.partecipating.execute((old) -> {
                    this.newKnownHostsCallback.accept(token.getKnownHostsMap());
                    return false;
                });
                this.endDiscoveryCallback.run();
                return token;
            }
        }
    }

    public boolean isDiscovering() {
        return this.partecipating.value();
    }

    public boolean executeIfIsDiscovering(Runnable runnable) {
        return this.partecipating.executeOnTrue(runnable);
    }

}
