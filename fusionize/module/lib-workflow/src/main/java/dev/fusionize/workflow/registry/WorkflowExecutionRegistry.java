package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;

import java.util.List;

public interface WorkflowExecutionRegistry {
    List<WorkflowExecution> getWorkflowExecutions(String workflowId);
    WorkflowExecution getWorkflowExecution(String workflowExecutionId);
    WorkflowExecution register(WorkflowExecution workflowExecution);
}
