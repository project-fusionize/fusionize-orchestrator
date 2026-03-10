package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

/**
 * A mock task component that always fails during run().
 * Used to test compensation and error handling flows.
 */
public final class MockFailingTaskComponent implements ComponentRuntime {

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockFailingTaskComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockFailingTaskComponent about to fail");
        emitter.failure(new RuntimeException("Simulated task failure"));
    }
}
