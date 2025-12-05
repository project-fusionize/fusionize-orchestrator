package dev.fusionize.workflow.component;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncComponentExecutorConfig {

    @Bean(name = "componentExecutor")
    public ExecutorService componentExecutor() {
        // Creates a thread pool that creates new threads as needed, but will reuse
        // previously constructed threads when they are available.
        // This is suitable for tasks that might block.
        return Executors.newCachedThreadPool();
    }
}
