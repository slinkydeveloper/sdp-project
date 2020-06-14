package com.slinkydeveloper.sdp.node.acquisition;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.node.simulator.Buffer;
import com.slinkydeveloper.sdp.node.simulator.Measurement;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a Buffer implementation that reduces the
 * values in the buffer following an overlapping sliding window.
 * <p>
 * Thread safety: Only one thread can add measurements, but more threads can concurrently access
 * to the available reduced values
 *
 * @param <T> Reduced values type
 */
public class OverlappingSlidingWindowBuffer<T> implements Buffer {

    private final static Logger LOG = LoggerConfig.getLogger(OverlappingSlidingWindowBuffer.class);

    public static final Function<Stream<Measurement>, Double> AVERAGE_REDUCER = measurementStream ->
        measurementStream
            .collect(Collectors.averagingDouble(Measurement::getValue));

    // Bounded Ring buffer implementation for measurements
    private final Measurement[] measurements;
    private int measurementsWriteIndex;
    private int written;
    private final int overlapNumber;

    // Measurements enqueued
    private final Queue<T> reducedMeasurements;
    private final Function<Stream<Measurement>, T> reducer;

    public OverlappingSlidingWindowBuffer(int slidingWindowCapacity, double overlap, Function<Stream<Measurement>, T> reducer) {
        Objects.requireNonNull(reducer);
        this.measurements = new Measurement[slidingWindowCapacity];
        this.overlapNumber = (int) (slidingWindowCapacity * overlap);
        this.reducedMeasurements = new ArrayDeque<>();
        this.reducer = reducer;
    }

    /**
     * Check if there are any reduced measurements.
     * If you need to perform polling, avoid checking with this method and then
     * perform polling, but just invoke {@link OverlappingSlidingWindowBuffer#pollReducedMeasurement()}
     *
     * @return true if there are any reduced measurements
     */
    public boolean hasReducedMeasurements() {
        synchronized (reducedMeasurements) {
            return !reducedMeasurements.isEmpty();
        }
    }

    /**
     * Non waiting poll for a reduced measurement
     *
     * @return Empty if there isn't any reduced measurement, otherwise returns the head of the queue
     */
    public Optional<T> pollReducedMeasurement() {
        synchronized (reducedMeasurements) {
            return Optional.ofNullable(reducedMeasurements.poll());
        }
    }

    /**
     * Note: this method is not thread-safe, only a single thread can add measurements
     *
     * @param m measurement to add
     */
    @Override
    public void addMeasurement(Measurement m) {
        this.measurements[this.measurementsWriteIndex] = m;
        this.measurementsWriteIndex = (this.measurementsWriteIndex + 1) % this.measurements.length;
        this.written++;
        LOG.finest("Measurement: " + m.getValue() + ", timestamp: " + m.getTimestamp() + ", written: " + this.written);
        if (this.written >= this.measurements.length && (this.written % this.overlapNumber == 0)) {
            generateNewReducedMeasurement();
        }
    }

    private void generateNewReducedMeasurement() {
        T reduced = this.reducer.apply(Arrays.stream(this.measurements));
        LOG.finest("New reduced value " + reduced.toString());
        synchronized (reducedMeasurements) {
            this.reducedMeasurements.add(reduced);
        }
    }
}
