package com.slinkydeveloper.sdp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.slinkydeveloper.sdp.SetUtils;

import java.util.Objects;
import java.util.Set;

public class NetworkTopologyChangeEvent {

    private final Set<Node> newNodes;
    private final Set<Node> oldNodes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NetworkTopologyChangeEvent(@JsonProperty("newNodes") Set<Node> newNodes, @JsonProperty("oldNodes") Set<Node> oldNodes) {
        this.newNodes = newNodes;
        this.oldNodes = oldNodes;
    }

    public Set<Node> getNewNodes() {
        return newNodes;
    }

    public Set<Node> getOldNodes() {
        return oldNodes;
    }

    @JsonIgnore
    public Set<Node> getAddedNodes() {
        return SetUtils.minus(newNodes, oldNodes);
    }

    @JsonIgnore
    public Set<Node> getRemovedNodes() {
        return SetUtils.minus(oldNodes, newNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkTopologyChangeEvent that = (NetworkTopologyChangeEvent) o;
        return Objects.equals(getNewNodes(), that.getNewNodes()) &&
            Objects.equals(getOldNodes(), that.getOldNodes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNewNodes(), getOldNodes());
    }

    @Override
    public String toString() {
        return "NetworkTopologyChangeEvent{" +
            "newNodes=" + newNodes +
            ", oldNodes=" + oldNodes +
            '}';
    }
}
