package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.Map;
import java.util.Optional;

/**
 * Simulates the END component of the workflow.
 * Logs its activation and publishes completion asynchronously.
 */
public final class MockEndFileComponent implements ComponentRuntime {

    public MockEndFileComponent() {
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().info("MockEndFileComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(100);
            Optional<String> extractedMap = context.varString("extracted");
            if (extractedMap.isPresent()) {
                emitter.logger().info("ComponentFinishedEvent finished with data {}",
                        extractedMap.get());
                emitter.success(context);
            }else {
                emitter.failure(new Exception("no extracted"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
