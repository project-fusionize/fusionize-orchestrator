package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;

public interface ComponentRuntime {
    void configure(ComponentRuntimeConfig config);
    void canActivate(Context context, ComponentUpdateEmitter emitter);
    void run(Context context, ComponentUpdateEmitter emitter);
}
