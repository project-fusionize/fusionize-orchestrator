package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

public class SendEmail implements ComponentRuntime {

    @Override
    public void configure(ComponentRuntimeConfig config) {}

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.logger().debug("MockSndEmailComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(1000);
            emitter.logger().warn("MockSndEmailComponent finished");
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
