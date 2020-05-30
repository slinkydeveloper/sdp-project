package com.slinkydeveloper.sdp.node.acquisition;

import com.slinkydeveloper.sdp.node.simulator.PM10Simulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class OverlappingSlidingWindowBufferTest {

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    void testIntegrationWithPM10Simulator() throws InterruptedException {
        OverlappingSlidingWindowBuffer<Double> buffer = new OverlappingSlidingWindowBuffer<>(
                10,
                0.5,
                OverlappingSlidingWindowBuffer.AVERAGE_REDUCER
        );
        PM10Simulator simulator = new PM10Simulator(buffer);

        // Start piling up measurements
        simulator.start();

        // Let's wait a bit
        while (!buffer.hasReducedMeasurements()) {
            // No need to continuously loop
            Thread.sleep(1000);
        }
        assertThat(buffer.pollReducedMeasurement())
                .isNotEmpty();

        // Let's try again
        while (!buffer.hasReducedMeasurements()) {
            // No need to continuously loop
            Thread.sleep(1000);
        }
        assertThat(buffer.pollReducedMeasurement())
                .isNotEmpty();
    }

}
