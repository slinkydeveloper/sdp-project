package com.slinkydeveloper.sdp.gateway.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SensorDataStatistics {

    private final List<Map.Entry<ZonedDateTime, SensorDataAverage>> dataAverages;
    private final double average;
    private final double standardDeviation;

    public SensorDataStatistics(List<Map.Entry<ZonedDateTime, SensorDataAverage>> dataAverages, double average, double standardDeviation) {
        this.dataAverages = dataAverages;
        this.average = average;
        this.standardDeviation = standardDeviation;
    }

    public List<Map.Entry<ZonedDateTime, SensorDataAverage>> getDataAverages() {
        return dataAverages;
    }

    public double getAverage() {
        return average;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorDataStatistics that = (SensorDataStatistics) o;
        return Double.compare(that.getAverage(), getAverage()) == 0 &&
            Double.compare(that.getStandardDeviation(), getStandardDeviation()) == 0 &&
            Objects.equals(getDataAverages(), that.getDataAverages());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataAverages(), getAverage(), getStandardDeviation());
    }

    @Override
    public String toString() {
        return "SensorDataStatistics{" +
            "readingAverages=" + dataAverages +
            ", average=" + average +
            ", standardDeviation=" + standardDeviation +
            '}';
    }


}
