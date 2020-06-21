package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.concurrent.AtomicList;
import com.slinkydeveloper.sdp.concurrent.AtomicMap;
import com.slinkydeveloper.sdp.gateway.model.Node;
import com.slinkydeveloper.sdp.gateway.model.SensorDataAverage;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class DataRepository {

    private final AtomicMap<Integer, String> hosts;
    private final AtomicList<Entry<ZonedDateTime, SensorDataAverage>> sensorData;

    private DataRepository() {
        this.hosts = new AtomicMap<>("Hosts");
        this.sensorData = new AtomicList<>("Sensor readings");
    }

    // Lazy initialization of singleton pattern
    // Class initialization operations performed by JVM are already synchronized, so there is no need to synchronize the singleton instantiation
    // https://coderanch.com/t/500367/java/synchronization-Lazy-Initialization
    private static class SingletonContainer {
        private final static DataRepository INSTANCE = new DataRepository();
    }

    private static DataRepository getInstance() {
        return DataRepository.SingletonContainer.INSTANCE;
    }

    public static AtomicMap<Integer, String> getHosts() {
        return getInstance().hosts;
    }

    public static Set<Node> getNodesSet() {
        return getHosts()
            .getCopy()
            .entrySet()
            .stream()
            .map(e -> new Node(e.getKey(), e.getValue()))
            .collect(Collectors.toSet());
    }

    public static AtomicList<Entry<ZonedDateTime, SensorDataAverage>> getSensorData() {
        return getInstance().sensorData;
    }
}
