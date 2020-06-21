package com.slinkydeveloper.sdp.gateway.model;

import java.util.Objects;
import java.util.Set;

public class SensorDataAverage {

    private final Set<Integer> participatingNodes;
    private final Double average;

    public SensorDataAverage(Set<Integer> participatingNodes, Double average) {
        this.participatingNodes = participatingNodes;
        this.average = average;
    }

    public Set<Integer> getParticipatingNodes() {
        return participatingNodes;
    }

    public Double getAverage() {
        return average;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorDataAverage that = (SensorDataAverage) o;
        return Objects.equals(getParticipatingNodes(), that.getParticipatingNodes()) &&
            Objects.equals(getAverage(), that.getAverage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParticipatingNodes(), getAverage());
    }

    @Override
    public String toString() {
        return "SensorDataAverage{" +
            "participatingNodes=" + participatingNodes +
            ", average=" + average +
            '}';
    }
}
