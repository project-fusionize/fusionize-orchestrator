package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;

import java.util.List;

public interface WorkflowExecutionRegistry {
    List<WorkflowExecution> getWorkflowExecutions(String workflowId);
    WorkflowExecution getWorkflowExecution(String workflowExecutionId);
    WorkflowExecution register(WorkflowExecution workflowExecution);
    void updateNodeExecution(String workflowExecutionId, WorkflowNodeExecution nodeExecution);
    void updateStatus(String workflowExecutionId, WorkflowExecutionStatus status);
}
