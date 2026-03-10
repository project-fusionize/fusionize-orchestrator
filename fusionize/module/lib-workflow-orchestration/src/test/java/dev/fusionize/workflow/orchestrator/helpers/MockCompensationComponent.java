package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

/**
 * A mock compensation component that logs its execution.
 * Used to verify compensation nodes are triggered on failure.
 */
public final class MockCompensationComponent implements ComponentRuntime {

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockCompensationComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockCompensationComponent executing compensation");
        emitter.success(context);
    }
}
