package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.repo.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkflowRepoRegistry implements WorkflowRegistry {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRepoRegistry.class);
    private final WorkflowRepository repository;

    public WorkflowRepoRegistry(WorkflowRepository repository) {
        this.repository = repository;
    }

    @Override
    public Workflow getWorkflow(String workflowExecutionId) {
        if (!StringUtils.hasText(workflowExecutionId)) return null;
        return repository.findByWorkflowId(workflowExecutionId).orElse(null);
    }

    public Workflow getWorkflowByDomain(String workflowDomain) {
        if (!StringUtils.hasText(workflowDomain)) return null;
        return repository.findByDomain(workflowDomain.toLowerCase()).orElse(null);
    }

    @Override
    public Workflow register(Workflow workflow) {
        if (workflow == null) return null;

        try {
            return repository.save(workflow);
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicate key detected for workflowId='{}' or domain='{}'. Attempting upsert.",
                    workflow.getWorkflowId(), workflow.getDomain());

            Workflow existing = null;
            if (StringUtils.hasText(workflow.getWorkflowId())) {
                existing = repository.findByWorkflowId(workflow.getWorkflowId()).orElse(null);
            }
            if (existing == null && StringUtils.hasText(workflow.getDomain())) {
                existing = repository.findByDomain(workflow.getDomain()).orElse(null);
            }

            if (existing != null) {
                existing.mergeFrom(workflow);
                try {
                    return repository.save(existing);
                } catch (Exception saveEx) {
                    log.error("Upsert failed after duplicate key detection for workflow '{}'.",
                            workflow.getWorkflowId(), saveEx);
                    throw saveEx;
                }
            } else {
                log.error("Duplicate key exception occurred, but no existing document found for workflowId='{}' or domain='{}'. Re-throwing.",
                        workflow.getWorkflowId(), workflow.getDomain());
                throw ex;
            }

        } catch (Exception e) {
            log.error("Failed to register workflow '{}'", workflow.getWorkflowId(), e);
            throw e;
        }
    }
}
