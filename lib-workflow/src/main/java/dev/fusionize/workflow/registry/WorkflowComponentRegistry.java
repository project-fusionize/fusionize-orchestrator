package dev.fusionize.workflow.registry;


import dev.fusionize.workflow.component.WorkflowComponent;

public interface WorkflowComponentRegistry {
    WorkflowComponent getWorkflowComponentById(String workflowComponentId);
    WorkflowComponent getWorkflowComponentByDomain(String workflowComponentId);
    WorkflowComponent register(WorkflowComponent workflowComponent);
}
