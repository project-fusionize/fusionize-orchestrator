package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;

import java.util.function.Predicate;

public interface WorkflowComponentRuntimeWait extends WorkflowComponentRuntime {
    void wait(WorkflowContext context, Predicate<WorkflowContext> onResume);
}
