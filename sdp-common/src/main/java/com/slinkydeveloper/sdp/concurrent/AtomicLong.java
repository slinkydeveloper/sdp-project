package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.logging.Logger;

public class AtomicLong {

    private final static Logger LOG = LoggerConfig.getLogger(AtomicLong.class);

    final String name;
    long value;

    public AtomicLong(String name, long initialValue) {
        this.name = name;
        this.value = initialValue;
    }

    public synchronized long getAndIncrement() {
        return getAndIncrement(1);
    }

    public synchronized long getAndIncrement(long i) {
        LOG.finest("Incrementing '" + name + "' by " + i);
        long old = this.value;
        this.value += i;
        return old;
    }

    public synchronized long get() {
        return this.value;
    }

    /**
     * Sets the {@code newValue} if greater than actual value. Returns the old value
     *
     * @param newValue
     * @return
     */
    public synchronized long getAndSetIfGreater(long newValue) {
        long old = this.value;
        if (newValue > this.value) {
            this.value = newValue;
        }
        return old;
    }
}
