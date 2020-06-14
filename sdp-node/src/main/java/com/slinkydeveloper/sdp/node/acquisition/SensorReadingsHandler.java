package com.slinkydeveloper.sdp.node.acquisition;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.SensorsReadingsToken;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class SensorReadingsHandler {

    private final static Logger LOG = LoggerConfig.getLogger(SensorReadingsHandler.class);

    private final int myId;
    private final OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer;

    public SensorReadingsHandler(int myId, OverlappingSlidingWindowBuffer<Double> slidingWindowBuffer) {
        this.myId = myId;
        this.slidingWindowBuffer = slidingWindowBuffer;
    }

    public SensorsReadingsToken handleSensorsReadingsToken(final SensorsReadingsToken request, final Set<Integer> knownHosts) {
        SensorsReadingsToken token = request;
        if (!request.containsLastMeasurements(this.myId)) {
            LOG.fine("Token does not contain data from myself");
            Optional<Double> newAverage = slidingWindowBuffer.pollReducedMeasurement();
            LOG.fine("New average: " + newAverage);
            if (newAverage.isPresent()) {
                token = token.toBuilder().putLastMeasurements(this.myId, newAverage.get()).build();
            }
        } else {
            LOG.fine("Token already contains data from myself");
        }

        if (token.getLastMeasurementsMap().keySet().containsAll(knownHosts)) {
            LOG.info("We have data from everybody, I'm going to send values to the gateway");
            // TODO send data to gateway

            return SensorsReadingsToken.newBuilder().build();
        }
        return token;
    }

}
