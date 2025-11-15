package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkflowExecutionRepoRegistry implements WorkflowExecutionRegistry {
    private final WorkflowExecutionRepository repository;

    public WorkflowExecutionRepoRegistry(WorkflowExecutionRepository repository) {
        this.repository = repository;
    }

    public WorkflowExecution getWorkflowExecution(String workflowExecutionId) {
        return repository.findByWorkflowExecutionId(workflowExecutionId).orElse(null);
    }

    public WorkflowExecution register(WorkflowExecution workflowExecution) {
        return repository.save(workflowExecution);
    }
}
