package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowContext;
import dev.fusionize.workflow.WorkflowNode;

import java.util.List;
import java.util.function.Predicate;

public interface WorkflowComponentRuntimeDecision extends WorkflowComponentRuntime {
    void decide(WorkflowContext context, Predicate<List<WorkflowNode>> decision);
}
