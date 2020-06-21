package com.slinkydeveloper.sdp.node.network;

public enum DiscoveryStatus {
    NOT_PARTICIPATING,
    NORMAL_PARTICIPATION,
    GENERATE_TOKEN_PARTICIPATION;

    public boolean isParticipating() {
        return this == NORMAL_PARTICIPATION || this == GENERATE_TOKEN_PARTICIPATION;
    }
}
