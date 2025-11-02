package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.repo.WorkflowRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRegistry {
    private final WorkflowRepository repository;

    public WorkflowRegistry(WorkflowRepository repository) {
        this.repository = repository;
    }

    public Workflow getWorkflow(String workflowExecutionId) {
        return repository.findByWorkflowId(workflowExecutionId).orElse(null);
    }

    public Workflow register(Workflow workflowExecution) {
        return repository.save(workflowExecution);
    }
}
