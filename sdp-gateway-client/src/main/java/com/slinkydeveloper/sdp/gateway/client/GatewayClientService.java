package com.slinkydeveloper.sdp.gateway.client;

import com.slinkydeveloper.sdp.model.NetworkTopologyChangeEvent;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataAverage;
import com.slinkydeveloper.sdp.model.SensorDataStatistics;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Interface describing the possible operations of the Gateway Client service
 */
public interface GatewayClientService {

    /**
     * @return the connected nodes. Note that the result of this method is eventually consistent, because a node may crash
     * while you're querying the set of nodes
     */
    Set<Node> nodes();

    /**
     * @return the statistics of all values if {@code limit == null}, otherwise the statistics of last {@code limit} values
     */
    SensorDataStatistics data(Integer limit);

    /**
     * Start and register consumers
     *
     * @return the function to close the runnable
     */
    Runnable registerEventHandler(
        Consumer<NetworkTopologyChangeEvent> networkTopologyChangeEventConsumer,
        Consumer<SensorDataAverage> newAverageConsumer
    );
}
