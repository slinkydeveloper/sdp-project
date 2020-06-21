package com.slinkydeveloper.sdp.gateway.model;

import java.util.Objects;
import java.util.Set;

public class SensorReadingsAverage {

    private final Set<Integer> partecipatingNodes;
    private final Double average;

    public SensorReadingsAverage(Set<Integer> partecipatingNodes, Double average) {
        this.partecipatingNodes = partecipatingNodes;
        this.average = average;
    }

    public Set<Integer> getPartecipatingNodes() {
        return partecipatingNodes;
    }

    public Double getAverage() {
        return average;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorReadingsAverage that = (SensorReadingsAverage) o;
        return Objects.equals(getPartecipatingNodes(), that.getPartecipatingNodes()) &&
            Objects.equals(getAverage(), that.getAverage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPartecipatingNodes(), getAverage());
    }

    @Override
    public String toString() {
        return "SensorNetworkReadingsAverage{" +
            "partecipatingNodes=" + partecipatingNodes +
            ", average=" + average +
            '}';
    }
}
