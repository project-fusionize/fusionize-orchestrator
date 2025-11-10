package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;

public interface WorkflowExecutionRegistry {
    WorkflowExecution getWorkflowExecution(String workflowExecutionId);
    WorkflowExecution register(WorkflowExecution workflowExecution);
}
