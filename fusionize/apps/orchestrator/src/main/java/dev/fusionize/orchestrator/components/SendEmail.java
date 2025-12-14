package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendEmail implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);

    @Override
    public void configure(ComponentRuntimeConfig config) {}

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        logger.info("MockSndEmailComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(1000);
            logger.info("MockSndEmailComponent finished");
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
