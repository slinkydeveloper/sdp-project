package com.slinkydeveloper.sdp.gateway.client;

import java.util.Map;

/**
 * Interface describing the possible operations of the Gateway service
 */
public interface GatewayNodeService {

    Map<Integer, String> join(int myId, String myAddress);

    void publishNewAverage(int senderId, Map<Integer, Double> readings);

    void publishNewHosts(int senderId, Map<Integer, String> hosts);
}
