package com.slinkydeveloper.sdp.node.discovery;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.DiscoveryToken;
import com.slinkydeveloper.sdp.node.DiscoveryTokenType;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This class is not thread safe
 */
public class DiscoveryStateMachine {

    private final static Logger LOG = LoggerConfig.getLogger(DiscoveryStateMachine.class);

    private final int myId;
    private final String myAddress;
    private final Consumer<Map<Integer, String>> endDiscoveryCallback;

    private boolean partecipating;

    public DiscoveryStateMachine(int myId, String myAddress, Consumer<Map<Integer, String>> endDiscoveryCallback) {
        this.myId = myId;
        this.myAddress = myAddress;
        this.endDiscoveryCallback = endDiscoveryCallback;

        this.partecipating = false;
    }

    /**
     * Generate the token to start the discovery from this node
     */
    public DiscoveryToken startDiscovery() {
        LOG.fine("Generating start discovery token");
        this.partecipating = true;
        return DiscoveryToken.newBuilder()
            .setType(DiscoveryTokenType.DISCOVERY)
            .setLeader(this.myId)
            .putKnownHosts(this.myId, this.myAddress)
            .build();
    }

    /**
     * Compute next token to send to the next neighbour
     *
     * @param token received token
     * @return the token to send to the next neighbour, if any
     */
    public DiscoveryToken onReceivedDiscovery(DiscoveryToken token) {
        LOG.fine("New message: \n" + token.toString());
        // Algorithm implemented as https://en.wikipedia.org/wiki/Chang_and_Roberts_algorithm#The_algorithm
        // but tweaked to do the service discovery
        if (token.getType() == DiscoveryTokenType.DISCOVERY) {
            // Add myself in the new token
            DiscoveryToken.Builder newTokenBuilder = token
                .toBuilder()
                .putKnownHosts(this.myId, this.myAddress);

            if (token.getLeader() > this.myId) {
                this.partecipating = true;
                return newTokenBuilder.build();
            } else if (token.getLeader() < this.myId) {
                if (this.partecipating) {
                    LOG.fine("Discarding message because I'm already partecipating and the leader id is lower than mine");
                    return null;
                } else {
                    this.partecipating = true;
                    return newTokenBuilder.setLeader(this.myId).build();
                }
            } else {
                LOG.fine("Discovery phase completed. Notifying results to the network");

                this.partecipating = false;
                // Notify the new known hosts
                this.endDiscoveryCallback.accept(token.getKnownHostsMap());
                return newTokenBuilder
                    .setType(DiscoveryTokenType.DISCOVERED)
                    .build();
            }
        } else {
            if (token.getLeader() == this.myId) {
                LOG.fine("Discarding message because the discovery is finished");
                return null;
            } else {
                this.partecipating = false;
                this.endDiscoveryCallback.accept(token.getKnownHostsMap());
                return token;
            }
        }
    }

    public boolean isDiscovering() {
        return this.partecipating;
    }

}
