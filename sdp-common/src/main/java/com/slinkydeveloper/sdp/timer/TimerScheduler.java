package com.slinkydeveloper.sdp.timer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.function.Predicate;

public class TimerScheduler {

    private final Map<String, Timer> timers;

    public TimerScheduler() {
        this.timers = new HashMap<>();
    }

    public synchronized void conditionalSchedule(Predicate<Set<String>> schedulePredicate, String id, long millis, Runnable runnable) {
        if (schedulePredicate.test(this.timers.keySet())) {
            Timer timer = new Timer(id, true);
            timer.schedule(new TimerTask(id, runnable), millis);
            cancelTimer(this.timers.put(id, timer));
        }
    }

    public synchronized void schedule(String id, long millis, Runnable runnable) {
        this.conditionalSchedule(v -> true, id, millis, runnable);
    }

    public synchronized void cancel(String id) {
        cancelTimer(this.timers.remove(id));
    }

    private void cancelTimer(Timer timer) {
        if (timer != null) {
            try {
                timer.cancel();
            } catch (IllegalStateException e) {
            }
        }
    }
}
