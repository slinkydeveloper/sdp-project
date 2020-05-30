package com.slinkydeveloper.sdp.node.acquisition;

import com.slinkydeveloper.sdp.node.simulator.Measurement;
import com.slinkydeveloper.sdp.node.simulator.PM10Simulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OverlappingSlidingWindowBufferTest {

    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    @Test
    void testCorrectAveraging() throws InterruptedException {
        OverlappingSlidingWindowBuffer<Double> buffer = new OverlappingSlidingWindowBuffer<>(
                10,
                0.5,
                OverlappingSlidingWindowBuffer.AVERAGE_REDUCER
        );

        for (int i = 0; i < 10; i++) {
            buffer.addMeasurement(new Measurement(i + "", "aaa", 10, System.currentTimeMillis()));
        }

        assertThat(buffer.pollReducedMeasurement())
                .hasValue(10d);

        for (int i = 0; i < 5; i++) {
            buffer.addMeasurement(new Measurement(i + "", "aaa", 6, System.currentTimeMillis()));
        }

        assertThat(buffer.pollReducedMeasurement())
                .hasValue(8d);
    }

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
