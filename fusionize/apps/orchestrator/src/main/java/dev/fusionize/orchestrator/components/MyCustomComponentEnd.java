package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyCustomComponentEnd implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MyCustomComponentEnd.class);

    @Override
    public void configure(ComponentRuntimeConfig config) {}

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        logger.info("MockEndEmailComponent activated");
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(10000);
            logger.info("ComponentFinishedEvent finished");
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
