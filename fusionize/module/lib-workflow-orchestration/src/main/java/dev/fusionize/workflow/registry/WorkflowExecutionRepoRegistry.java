package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowExecutionRepoRegistry implements WorkflowExecutionRegistry {
    private final WorkflowExecutionRepository repository;

    public WorkflowExecutionRepoRegistry(WorkflowExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WorkflowExecution> getWorkflowExecutions(String workflowId) {
        return repository.findByWorkflowIdIn(List.of(workflowId));
    }

    public WorkflowExecution getWorkflowExecution(String workflowExecutionId) {
        return repository.findByWorkflowExecutionId(workflowExecutionId).orElse(null);
    }

    public WorkflowExecution register(WorkflowExecution workflowExecution) {
        return repository.save(workflowExecution);
    }
}
