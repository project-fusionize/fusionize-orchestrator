package dev.fusionize.workflow.component.registery;


import dev.fusionize.workflow.component.WorkflowComponent;

import java.util.List;

public interface WorkflowComponentRegistry {
    List<WorkflowComponent> getComponents();
    WorkflowComponent getWorkflowComponentById(String workflowComponentId);
    WorkflowComponent getWorkflowComponentByDomain(String workflowComponentId);
    WorkflowComponent register(WorkflowComponent workflowComponent);
}
