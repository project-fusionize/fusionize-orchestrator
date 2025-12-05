package dev.fusionize.workflow.component.registery;


import dev.fusionize.workflow.component.WorkflowComponent;

public interface WorkflowComponentRegistry {
    WorkflowComponent getWorkflowComponentById(String workflowComponentId);
    WorkflowComponent getWorkflowComponentByDomain(String workflowComponentId);
    WorkflowComponent register(WorkflowComponent workflowComponent);
}
