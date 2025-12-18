package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.function.Consumer;

/**
 * Simulates the END component of the workflow.
 * Logs its activation and publishes completion asynchronously.
 */
public final class MockWaitUtilComponent implements ComponentRuntime {
    public static class Beacon {
        private Consumer<String> consumer;

        public void send(String message) {
            if(consumer != null) {
                consumer.accept(message);
            }
        }
    }

    private final Beacon beacon;
    public MockWaitUtilComponent(Beacon beacon) {
        this.beacon = beacon;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockWaitUtilComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        beacon.consumer = (String message) -> {
            context.set("beacon", message);
            emitter.logger().info(message);
            emitter.success(context);
        };
    }
}
