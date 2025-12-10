package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

public class NoopComponent implements LocalComponentRuntime {
    public static final String NAME = "noop";

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        emitter.success(context);
    }
}
