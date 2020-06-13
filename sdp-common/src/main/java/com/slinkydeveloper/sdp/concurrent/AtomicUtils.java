package com.slinkydeveloper.sdp.concurrent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AtomicUtils {

    public static boolean atomicExecuteOnPredicate(Object lock, Supplier<Boolean> predicate, Consumer<Boolean> executeInLockedSection) {
        boolean result;
        synchronized (lock) {
            result = predicate.get();
            executeInLockedSection.accept(result);
        }
        return result;
    }

    public static boolean atomicExecuteOnPredicateSuccess(Object lock, Supplier<Boolean> predicate, Runnable executeInLockedSection) {
        return atomicExecuteOnPredicate(lock, predicate, res -> {
            if (res) {
                executeInLockedSection.run();
            }
        });
    }

    public static boolean atomicCheckPredicate(Object lock, Supplier<Boolean> predicate) {
        return atomicExecuteOnPredicate(lock, predicate, (v) -> {
        });
    }

    public static <T> T atomicGet(final Object lock, Supplier<T> supplier) {
        T result;
        synchronized (lock) {
            result = supplier.get();
        }
        return result;
    }

}
