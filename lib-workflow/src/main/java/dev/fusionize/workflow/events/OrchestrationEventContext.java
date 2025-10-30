package dev.fusionize.workflow.events;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;

public class OrchestrationEventContext {
    private final WorkflowExecution workflowExecution;
    private final WorkflowNodeExecution nodeExecution;


    public OrchestrationEventContext(WorkflowExecution workflowExecution, WorkflowNodeExecution nodeExecution) {
        this.workflowExecution = workflowExecution;
        this.nodeExecution = nodeExecution;
    }

    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }

    public WorkflowNodeExecution getNodeExecution() {
        return nodeExecution;
    }
}
