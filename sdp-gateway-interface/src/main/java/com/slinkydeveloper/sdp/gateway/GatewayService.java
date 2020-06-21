package com.slinkydeveloper.sdp.gateway;

import java.util.Map;

/**
 * Interface describing the possible operations of the Gateway service
 */
public interface GatewayService {

    void publishNewSensorReadings(int senderId, Map<Integer, Double> readings);

    void publishNewHosts(int senderId, Map<Integer, String> hosts);
}
