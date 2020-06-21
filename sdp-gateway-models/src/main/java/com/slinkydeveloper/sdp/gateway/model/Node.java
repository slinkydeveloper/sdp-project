package com.slinkydeveloper.sdp.gateway.model;

import java.util.Objects;

public class Node {

    private final int id;
    private final String host;

    public Node(int id, String host) {
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
