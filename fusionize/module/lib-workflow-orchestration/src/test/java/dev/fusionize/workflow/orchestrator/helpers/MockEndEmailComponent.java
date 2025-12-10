package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.Optional;

/**
 * Simulates the END component of the workflow.
 * Logs its activation and publishes completion asynchronously.
 */
public final class MockEndEmailComponent implements ComponentRuntime {

    public MockEndEmailComponent() {
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockEndEmailComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(200);
            Optional<Integer> delayFromLocal = context.var(DelayComponent.VAR_DELAYED,
                    Integer.class);
            emitter.logger().info("ComponentFinishedEvent finished after {}",
                    delayFromLocal.orElse(-1));
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
