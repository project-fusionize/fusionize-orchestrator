package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;

public interface WorkflowComponentRuntime {
    void configure(WorkflowComponentConfig config);
    boolean canActivate(WorkflowContext context);
}
