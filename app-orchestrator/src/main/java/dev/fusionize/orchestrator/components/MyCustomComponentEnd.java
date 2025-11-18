package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.context.WorkflowContext;
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
    public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
        logger.info("MockEndEmailComponent activated");
        emitter.success(workflowContext);
    }

    @Override
    public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(10000);
            logger.info("ComponentFinishedEvent finished");
            emitter.success(workflowContext);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
