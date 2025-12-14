package dev.fusionize.common.utility;

import java.util.concurrent.*;

public class Debouncer<K> {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<K, Future<?>> delayedMap = new ConcurrentHashMap<>();
    private final long delay;
    private final TimeUnit timeUnit;

    public Debouncer(long delay, TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public void debounce(K key, Runnable runnable) {
        Future<?> prev = delayedMap.put(key, scheduler.schedule(() -> {
            try {
                runnable.run();
            } finally {
                delayedMap.remove(key);
            }
        }, delay, timeUnit));

        if (prev != null) {
            prev.cancel(false);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
