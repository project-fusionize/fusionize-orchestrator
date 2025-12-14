package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;

import java.util.List;

public interface WorkflowRegistry {
    Workflow getWorkflow(String workflowExecutionId);

    Workflow register(Workflow workflowExecution);

    List<Workflow> getAll();

    Workflow getWorkflowByDomain(String domain);
}
