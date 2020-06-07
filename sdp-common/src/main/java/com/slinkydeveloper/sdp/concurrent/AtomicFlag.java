package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.logging.Logger;

public class AtomicFlag {

    private final static Logger LOG = LoggerConfig.getLogger(AtomicFlag.class);

    final String name;
    boolean value;

    public AtomicFlag(String name, boolean initialValue) {
        this.name = name;
        this.value = initialValue;
    }

    public synchronized void setTrue() {
        LOG.fine("Setting flag " + name + " to true");
        this.value = true;
    }

    public synchronized void setFalse() {
        LOG.fine("Setting flag " + name + " to false");
        this.value = false;
    }

    public synchronized boolean value() {
        return value;
    }
}
