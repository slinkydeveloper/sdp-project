package com.slinkydeveloper.sdp.node.acquisition;

import com.slinkydeveloper.sdp.gateway.client.GatewayNodeService;
import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.SensorReadingsToken;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class SensorReadingsHandler {

    private final static Logger LOG = LoggerConfig.getLogger(SensorReadingsHandler.class);

    private final int myId;
    private final OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer;
    private final GatewayNodeService gatewayService;

    public SensorReadingsHandler(int myId, OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer, GatewayNodeService gatewayService) {
        this.myId = myId;
        this.slidingWindowBuffer = slidingWindowBuffer;
        this.gatewayService = gatewayService;
    }

    public SensorReadingsToken handleSensorReadingsToken(final SensorReadingsToken request, final Set<Integer> knownHosts) {
        SensorReadingsToken token = request;
        if (!request.containsLastMeasurements(this.myId)) {
            LOG.fine("Token does not contain data from myself");
            Optional<Double> newAverage = slidingWindowBuffer.pollReducedMeasurement();
            if (newAverage.isPresent()) {
                token = token.toBuilder().putLastMeasurements(this.myId, newAverage.get()).build();
            }
        } else {
            LOG.fine("Token already contains data from myself");
        }

        if (token.getLastMeasurementsMap().keySet().containsAll(knownHosts)) {
            LOG.info("We have data from everybody, I'm going to send values to the gateway");

            gatewayService.publishNewAverage(this.myId, token.getLastMeasurementsMap());

            return SensorReadingsToken.newBuilder().setGenerationUUID(token.getGenerationUUID()).build();
        }
        return token;
    }

}
