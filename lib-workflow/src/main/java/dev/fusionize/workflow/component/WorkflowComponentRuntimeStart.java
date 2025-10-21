package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;

import java.util.function.Predicate;

public interface WorkflowComponentRuntimeStart extends WorkflowComponentRuntime {
    void start(Predicate<WorkflowContext> onTriggered);
}
