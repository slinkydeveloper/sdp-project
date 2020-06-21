package com.slinkydeveloper.sdp.gateway.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Node {

    private final int id;
    private final String host;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Node(@JsonProperty("id") int id, @JsonProperty("host") String host) {
        this.id = id;
        this.host = host;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return "Node{" +
            "id=" + id +
            ", host='" + host + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return getId() == node.getId() &&
            Objects.equals(getHost(), node.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getHost());
    }
}
