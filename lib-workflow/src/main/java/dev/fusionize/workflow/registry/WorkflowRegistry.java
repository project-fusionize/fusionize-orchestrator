package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;

public interface WorkflowRegistry {
    Workflow getWorkflow(String workflowExecutionId);

    Workflow register(Workflow workflowExecution);

    java.util.List<Workflow> getAll();

    Workflow getWorkflowByDomain(String domain);
}
