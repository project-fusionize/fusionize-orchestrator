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
            inflate(workflow);
        }
        return workflow;
    }

    @Override
    public Workflow getWorkflowByDomain(String workflowDomain) {
        if (!StringUtils.hasText(workflowDomain))
            return null;
        Workflow workflow = repository.findByDomain(workflowDomain.toLowerCase()).orElse(null);
        if (workflow != null) {
            inflate(workflow);
        }
        return workflow;
    }

    @Override
    public java.util.List<Workflow> getAll() {
        java.util.List<Workflow> workflows = repository.findAll();
        workflows.forEach(this::inflate);
        return workflows;
    }

    @Override
    public Workflow register(Workflow workflow) {
        if (workflow == null)
            return null;

        flatten(workflow);

        try {
            Workflow saved = repository.save(workflow);
            inflate(saved);
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
                existing.mergeFrom(workflow);
                flatten(existing); // Flatten again after merge
                try {
                    Workflow saved = repository.save(existing);
                    inflate(saved);
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

    private void flatten(Workflow workflow) {
        workflow.getNodeMap().clear();
        workflow.getRootNodeIds().clear();
        if (workflow.getNodes() != null) {
            for (WorkflowNode node : workflow.getNodes()) {
                workflow.getRootNodeIds().add(node.getWorkflowNodeId());
                flattenNode(node, workflow.getNodeMap());
            }
        }
    }

    private void flattenNode(WorkflowNode node, Map<String, WorkflowNode> nodeMap) {
        if (nodeMap.containsKey(node.getWorkflowNodeId())) {
            return; // Already processed (cycle)
        }
        nodeMap.put(node.getWorkflowNodeId(), node);
        node.getChildrenIds().clear();
        if (node.getChildren() != null) {
            for (WorkflowNode child : node.getChildren()) {
                node.getChildrenIds().add(child.getWorkflowNodeId());
                flattenNode(child, nodeMap);
            }
        }
    }

    private void inflate(Workflow workflow) {
        workflow.getNodes().clear();
        // First pass: link roots
        if (workflow.getRootNodeIds() != null) {
            for (String rootId : workflow.getRootNodeIds()) {
                WorkflowNode rootNode = workflow.getNodeMap().get(rootId);
                if (rootNode != null) {
                    workflow.getNodes().add(rootNode);
                }
            }
        }
        // Second pass: link children for all nodes
        for (WorkflowNode node : workflow.getNodeMap().values()) {
            node.getChildren().clear();
            if (node.getChildrenIds() != null) {
                for (String childId : node.getChildrenIds()) {
                    WorkflowNode child = workflow.getNodeMap().get(childId);
                    if (child != null) {
                        node.getChildren().add(child);
                    }
                }
            }
        }
    }
}
