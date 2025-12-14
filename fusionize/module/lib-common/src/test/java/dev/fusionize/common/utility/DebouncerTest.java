package dev.fusionize.common.utility;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DebouncerTest {

    @Test
    void debounce_shouldExecuteOnlyLastCall() {
        Debouncer<String> debouncer = new Debouncer<>(100, TimeUnit.MILLISECONDS);
        AtomicInteger counter = new AtomicInteger(0);

        debouncer.debounce("key1", counter::incrementAndGet);
        debouncer.debounce("key1", counter::incrementAndGet);
        debouncer.debounce("key1", counter::incrementAndGet);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(1, counter.get())
        );

        debouncer.shutdown();
    }

    @Test
    void debounce_shouldExecuteMultipleKeysIndependently() {
        Debouncer<String> debouncer = new Debouncer<>(100, TimeUnit.MILLISECONDS);
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        debouncer.debounce("key1", counter1::incrementAndGet);
        debouncer.debounce("key2", counter2::incrementAndGet);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, counter1.get());
            assertEquals(1, counter2.get());
        });

        debouncer.shutdown();
    }
}
