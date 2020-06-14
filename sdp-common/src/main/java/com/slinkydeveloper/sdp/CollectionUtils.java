package com.slinkydeveloper.sdp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CollectionUtils {

    /**
     * Returns the difference between initial and the operands
     *
     * @param initial
     * @param operands
     * @param <T>
     * @return
     */
    public static <T> Set<T> minus(Set<T> initial, Set<T>... operands) {
        initial = new HashSet<>(initial);
        for (Set<T> s : operands) {
            initial.removeAll(s);
        }
        return Collections.unmodifiableSet(initial);
    }

}
