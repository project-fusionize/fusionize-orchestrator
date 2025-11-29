package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DelayComponent implements LocalComponentRuntime {
    public static final String VAR_DELAYED = "delayed";
    public static final String CONF_DELAY = "delay";
    private int delay;

    // Shared scheduler for all DelayComponent instances to avoid creating too many
    // threads
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
        emitter.logger().info("scheduling delay of {} milliseconds", delay);
        scheduler.schedule(() -> {
            try {
                context.set(VAR_DELAYED, delay);
                emitter.success(context);
            } catch (Exception e) {
                emitter.failure(e);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
