package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.repo.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dev.fusionize.workflow.WorkflowNode;
import java.util.Map;

@Component
public class WorkflowRepoRegistry implements WorkflowRegistry {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRepoRegistry.class);
    private final WorkflowRepository repository;

    public WorkflowRepoRegistry(WorkflowRepository repository) {
        this.repository = repository;
    }

    @Override
    public Workflow getWorkflow(String workflowExecutionId) {
        if (!StringUtils.hasText(workflowExecutionId))
            return null;
        Workflow workflow = repository.findByWorkflowId(workflowExecutionId).orElse(null);
        if (workflow != null) {
            workflow.inflate();
        }
        return workflow;
    }

    @Override
    public Workflow getWorkflowByDomain(String workflowDomain) {
        if (!StringUtils.hasText(workflowDomain))
            return null;
        Workflow workflow = repository.findByDomain(workflowDomain.toLowerCase()).orElse(null);
        if (workflow != null) {
            workflow.inflate();
        }
        return workflow;
    }

    @Override
    public java.util.List<Workflow> getAll() {
        java.util.List<Workflow> workflows = repository.findAll();
        workflows.forEach(Workflow::inflate);
        return workflows;
    }

    @Override
    public Workflow register(Workflow workflow) {
        if (workflow == null)
            return null;

        workflow.flatten();

        try {
            Workflow saved = repository.save(workflow);
            saved.inflate();
            return saved;
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
                existing.inflate(); // Ensure existing is inflated before merge
                existing.mergeFrom(workflow); // mergeFrom now handles flattening/re-flattening internally
                
                try {
                    Workflow saved = repository.save(existing);
                    saved.inflate();
                    return saved;
                } catch (Exception saveEx) {
                    log.error("Upsert failed after duplicate key detection for workflow '{}'.",
                            workflow.getWorkflowId(), saveEx);
                    throw saveEx;
                }
            } else {
                log.error(
                        "Duplicate key exception occurred, but no existing document found for workflowId='{}' or domain='{}'. Re-throwing.",
                        workflow.getWorkflowId(), workflow.getDomain());
                throw ex;
            }

        } catch (Exception e) {
            log.error("Failed to register workflow '{}'", workflow.getWorkflowId(), e);
            throw e;
        }
    }
}
