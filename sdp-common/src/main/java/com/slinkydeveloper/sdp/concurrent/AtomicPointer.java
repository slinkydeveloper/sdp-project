package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.logging.Logger;

public class AtomicPointer<T> {

    private final static Logger LOG = LoggerConfig.getLogger(AtomicPointer.class);

    private final String name;
    private T value;

    public AtomicPointer(String name, T initialValue) {
        this.name = name;
        this.value = initialValue;
    }

    public AtomicPointer(String name) {
        this(name, null);
    }

    public synchronized void clear() {
        if (value != null) {
            LOG.fine("Cleared value " + name);
            this.value = null;
        }
    }

    public synchronized void set(T value) {
        LOG.fine("Setting value " + name);
        this.value = value;
    }

    public synchronized T get() {
        return this.value;
    }

    public synchronized T getAndClear() {
        T val = this.value;
        this.value = null;
        return val;
    }

    public synchronized boolean isEmpty() {
        return this.value == null;
    }
}
