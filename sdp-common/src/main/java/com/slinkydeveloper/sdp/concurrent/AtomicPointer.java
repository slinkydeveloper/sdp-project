package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.function.Consumer;
import java.util.function.Function;
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
            LOG.fine("Cleared value '" + name + "'");
            this.value = null;
        }
    }

    public synchronized void set(T value) {
        LOG.fine("Setting pointer '" + name + "' to " + this.value);
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

    /**
     * Returns the old value
     *
     * @param newValueGenerator
     * @return
     */
    public synchronized T swap(Function<T, T> newValueGenerator) {
        T old = this.value;
        set(newValueGenerator.apply(old));
        return old;
    }

    /**
     * Doesn't swap the internal value, returns the result of the function executed on the pointer lock
     *
     * @param fn
     * @return
     */
    public synchronized <U> U map(Function<T, U> fn) {
        return fn.apply(this.value);
    }

    /**
     * Consume the internal value in the pointer lock
     *
     * @param fn
     * @return
     */
    public synchronized void consume(Consumer<T> fn) {
        fn.accept(this.value);
    }
}
