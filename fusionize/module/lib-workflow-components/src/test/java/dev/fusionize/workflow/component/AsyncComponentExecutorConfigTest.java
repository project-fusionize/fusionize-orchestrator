package dev.fusionize.workflow.component;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncComponentExecutorConfigTest {

    private final AsyncComponentExecutorConfig config = new AsyncComponentExecutorConfig();

    @Test
    void shouldCreateExecutorService() {
        // setup
        var executor = config.componentExecutor();

        // expectation
        // the factory method should return a non-null ExecutorService instance

        // validation
        assertThat(executor).isNotNull();
        executor.shutdownNow();
    }

    @Test
    void shouldReturnNonShutdownExecutor() {
        // setup
        var executor = config.componentExecutor();

        // expectation
        // a freshly created executor should not be in shutdown state

        // validation
        assertThat(executor.isShutdown()).isFalse();
        executor.shutdownNow();
    }

    @Test
    void shouldAcceptAndExecuteTasks() throws InterruptedException {
        // setup
        var executor = config.componentExecutor();
        var latch = new CountDownLatch(1);

        // expectation
        executor.submit(latch::countDown);

        // validation
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
    }
}
