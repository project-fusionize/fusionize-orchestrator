package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;

import java.util.function.Predicate;

public interface WorkflowComponentRuntimeTask extends WorkflowComponentRuntime {
    void run(WorkflowContext context, Predicate<WorkflowContext> onFinish);

}
