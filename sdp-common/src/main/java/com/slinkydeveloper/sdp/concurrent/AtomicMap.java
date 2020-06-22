package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class AtomicMap<K, V> {

    private final static Logger LOG = LoggerConfig.getLogger(AtomicMap.class);

    private final String name;
    private final Map<K, V> map;

    public AtomicMap(String name, Map<K, V> initialValue) {
        this.name = name;
        this.map = initialValue != null ? new HashMap<>(initialValue) : new HashMap<>();
    }

    public AtomicMap(String name) {
        this(name, null);
    }

    public synchronized Map<K, V> getCopy() {
        return new HashMap<>(this.map);
    }

    public synchronized V put(K key, V value) {
        LOG.fine(() -> "Putting key '" + key + "' with value '" + value + "' in map '" + name + "'");
        return this.map.put(key, value);
    }

    public synchronized boolean putIf(K key, V value, Predicate<Map<K, V>> predicate) {
        if (predicate.test(Collections.unmodifiableMap(this.map))) {
            put(key, value);
            return true;
        }
        return false;
    }

    public synchronized V remove(K key) {
        LOG.fine("Removing key '" + key + "' from map '" + name + "'");
        return this.map.remove(key);
    }

    public synchronized Map<K, V> replaceAll(Map<K, V> newMap) {
        LOG.fine(() -> "Replacing map '" + name + "' with new values: " + newMap);
        Map<K, V> old = getCopy();
        this.map.clear();
        this.map.putAll(newMap);
        return old;
    }
}
