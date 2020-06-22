package com.slinkydeveloper.sdp.concurrent;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AtomicList<V> {

    private final static Logger LOG = LoggerConfig.getLogger(AtomicList.class);

    private final String name;
    private final List<V> list;

    public AtomicList(String name, List<V> initialValue) {
        this.name = name;
        this.list = initialValue != null ? new ArrayList<>(initialValue) : new ArrayList<>();
    }

    public AtomicList(String name) {
        this(name, null);
    }

    public synchronized List<V> getCopy() {
        return new ArrayList<>(this.list);
    }

    public synchronized List<V> getLast(int n) {
        if (n >= this.list.size()) {
            return getCopy();
        }
        return this.list.stream().skip(this.list.size() - n).collect(Collectors.toList());
    }

    public synchronized void append(V value) {
        LOG.fine(() -> "Appending value '" + value + "' in list '" + name + "'");
        this.list.add(value);
    }

    public synchronized V removeFirst() {
        return removeFirst(1).get(0);
    }

    public synchronized List<V> removeFirst(int n) {
        List<V> old = new ArrayList<>(n);
        while (n != 0) {
            old.add(this.list.remove(0));
            n--;
        }
        LOG.fine("Removed first '" + n + "' elements from list '" + name + "'");
        return old;
    }
}
