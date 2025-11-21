package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

public class DelayComponent implements LocalComponentRuntime {
    public static final String VAR_DELAYED= "delayed";
    public static final String CONF_DELAY = "delay";
    private int delay;

    @Override
    public void configure(ComponentRuntimeConfig config) {
        delay = config.getConfig().containsKey(CONF_DELAY) ? Integer.parseInt(
                config.getConfig().get(CONF_DELAY).toString()) : 5 * 1000;
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            emitter.logger().info("sleeping {} milliseconds", delay);
            Thread.sleep(delay);
            context.set(VAR_DELAYED, delay);
            emitter.success(context);
        } catch (InterruptedException e) {
            emitter.failure(e);
        }

    }
}
