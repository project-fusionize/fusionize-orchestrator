package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.repo.WorkflowComponentRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkflowComponentRepoRegistry implements WorkflowComponentRegistry{
    private final WorkflowComponentRepository repository;

    public WorkflowComponentRepoRegistry(WorkflowComponentRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkflowComponent getWorkflowComponentById(String workflowComponentId) {
        return this.repository.findById(workflowComponentId).orElse(null);
    }

    @Override
    public WorkflowComponent getWorkflowComponentByDomain(String workflowComponentDomain) {
        return this.repository.findByDomain(workflowComponentDomain).orElse(null);
    }

    @Override
    public WorkflowComponent register(WorkflowComponent workflowComponent) {
        return this.repository.save(workflowComponent);
    }

}
