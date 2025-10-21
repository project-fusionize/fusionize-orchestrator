package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;
import dev.fusionize.workflow.WorkflowNode;

public interface WorkflowComponentRuntime extends Cloneable {
    void configure(WorkflowNode runningNode);
    boolean canActivate(WorkflowContext context);
    WorkflowComponentRuntime clone() throws CloneNotSupportedException;
}
