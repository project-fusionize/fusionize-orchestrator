package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.repo.WorkflowComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkflowComponentRepoRegistry implements WorkflowComponentRegistry {

    private static final Logger log = LoggerFactory.getLogger(WorkflowComponentRepoRegistry.class);
    private final WorkflowComponentRepository repository;

    public WorkflowComponentRepoRegistry(WorkflowComponentRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkflowComponent getWorkflowComponentById(String workflowComponentId) {
        if (!StringUtils.hasText(workflowComponentId)) return null;
        return repository.findById(workflowComponentId).orElse(null);
    }

    @Override
    public WorkflowComponent getWorkflowComponentByDomain(String workflowComponentDomain) {
        if (!StringUtils.hasText(workflowComponentDomain)) return null;
        return repository.findByDomain(workflowComponentDomain.toLowerCase()).orElse(null);
    }

    @Override
    public WorkflowComponent register(WorkflowComponent workflowComponent) {
        if (workflowComponent == null) return null;

        try {
            return repository.save(workflowComponent);
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicate key detected for domain '{}'. Performing upsert instead.",
                    workflowComponent.getDomain());

            WorkflowComponent existing = repository.findByDomain(workflowComponent.getDomain()).orElse(null);
            if (existing != null) {
                existing.mergeFrom(workflowComponent);
                try {
                    return repository.save(existing);
                } catch (Exception saveEx) {
                    log.error("Upsert failed after duplicate key detection.", saveEx);
                    throw saveEx;
                }
            } else {
                log.error("Duplicate key exception occurred, but no existing document found. Re-throwing.");
                throw ex;
            }

        } catch (Exception e) {
            log.error("Failed to register workflow component '{}'", workflowComponent.getDomain(), e);
            throw e;
        }
    }
}
