package com.slinkydeveloper.sdp.gateway.client;

import com.slinkydeveloper.sdp.gateway.model.Node;
import com.slinkydeveloper.sdp.gateway.model.SensorDataStatistics;

import java.util.Set;

/**
 * Interface describing the possible operations of the Gateway Client service
 */
public interface GatewayClientService {

    Set<Node> nodes();

    SensorDataStatistics data(Integer n);
}
