package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;

public interface ComponentRuntime {
    void configure(ComponentRuntimeConfig config);
    void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter);
    void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter);
}
