package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;
import dev.fusionize.workflow.WorkflowExecutionStatus;

import java.util.function.Predicate;

public interface WorkflowComponentRuntimeEnd extends WorkflowComponentRuntime {
    void finish(WorkflowContext context, Predicate<WorkflowExecutionStatus> onEnd);
}
