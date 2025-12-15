package dev.fusionize.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.fusionize.common.utility.KeyUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties({"workflow"})
@Document(collection = "workflow-execution")
public class WorkflowExecution {
    @Id
    private String id;
    private String workflowExecutionId;
    private String workflowId;
    private Map<String, WorkflowNodeExecution> nodeExecutionMap = new HashMap<>();
    private List<String> rootNodeExecutionIds = new ArrayList<>();
    private WorkflowExecutionStatus status;
    private Instant createdDate;
    private Instant updatedDate;

    @Transient
    private Workflow workflow;
    @Transient
    private List<WorkflowNodeExecution> nodes = new ArrayList<>();

    public static WorkflowExecution of(Workflow workflow) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.workflow = workflow;
        execution.workflowId = workflow.getWorkflowId();
        execution.status = WorkflowExecutionStatus.IDLE;
        execution.workflowExecutionId = KeyUtil.getTimestampId("WEXE");
        execution.createdDate = Instant.now();
        execution.updatedDate = Instant.now();
        return execution;
    }

    public WorkflowNodeExecution findNodeByWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        if (nodeExecutionMap.containsKey(workflowNodeExecutionId)) {
            return nodeExecutionMap.get(workflowNodeExecutionId);
        }
        return nodes.stream()
                .filter(n -> n.getWorkflowNodeExecutionId().equals(workflowNodeExecutionId))
                .findFirst()
                .orElseGet(() -> nodes.stream()
                        .map(n -> n.findNodeByWorkflowNodeExecutionId(workflowNodeExecutionId))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    public List<WorkflowNodeExecution> findNodesByWorkflowNodeId(String workflowNodeId) {
        // Search in map first if populated
        if (!nodeExecutionMap.isEmpty()) {
            return nodeExecutionMap.values().stream()
                    .filter(n -> n.getWorkflowNodeId().equals(workflowNodeId))
                    .collect(Collectors.toList());
        }
        
        // Fallback to searching in tree
        List<WorkflowNodeExecution> nodesByWorkflowNodeId =  nodes.stream()
                .filter(n -> n.getWorkflowNodeId().equals(workflowNodeId)).toList();
        List<WorkflowNodeExecution> childrenNodesByWorkflowNodeId = nodes.stream()
                .map(n -> n.findNodesByWorkflowNodeId(workflowNodeId))
                .flatMap(Collection::stream).toList();
        return Stream.concat(nodesByWorkflowNodeId.stream(), childrenNodesByWorkflowNodeId.stream())
                .collect(Collectors.toList());
    }

    public WorkflowExecution renew() {
        WorkflowExecution clone = new WorkflowExecution();
        clone.id = null;
        clone.workflowExecutionId = KeyUtil.getTimestampId("WEXE");
        clone.workflowId = this.workflowId;
        clone.status = WorkflowExecutionStatus.IDLE;
        clone.workflow = this.workflow;
        clone.createdDate = Instant.now();
        clone.updatedDate = Instant.now();

        // Ensure current state is flattened
        this.flatten();
        
        List<WorkflowNodeExecution> renewedNodes = new ArrayList<>();
        // Renewing from tree structure is safer to preserve hierarchy logic initially
        // But we need to handle map based renewal too.
        // Let's rely on inflate() being called before renew() typically, or just renew from the map if nodes are empty
        
        if (this.nodes.isEmpty() && !this.nodeExecutionMap.isEmpty()) {
             this.inflate();
        }

        for (WorkflowNodeExecution node : this.nodes) {
            renewedNodes.add(node.renew());
        }
        clone.nodes = renewedNodes;
        clone.flatten(); // Prepare the clone with flattened structure

        return clone;
    }

    public void flatten() {
        this.nodeExecutionMap.clear();
        this.rootNodeExecutionIds.clear();
        if (this.nodes != null) {
            for (WorkflowNodeExecution node : this.nodes) {
                this.rootNodeExecutionIds.add(node.getWorkflowNodeExecutionId());
                flattenNode(node);
            }
        }
    }

    private void flattenNode(WorkflowNodeExecution node) {
        if (nodeExecutionMap.containsKey(node.getWorkflowNodeExecutionId())) {
            return;
        }
        nodeExecutionMap.put(node.getWorkflowNodeExecutionId(), node);
        node.getChildrenIds().clear();
        if (node.getChildren() != null) {
            for (WorkflowNodeExecution child : node.getChildren()) {
                node.getChildrenIds().add(child.getWorkflowNodeExecutionId());
                flattenNode(child);
            }
        }
    }

    public void inflate() {
        this.nodes.clear();
        // First pass: link roots
        if (this.rootNodeExecutionIds != null) {
            for (String rootId : this.rootNodeExecutionIds) {
                WorkflowNodeExecution rootNode = this.nodeExecutionMap.get(rootId);
                if (rootNode != null) {
                    this.nodes.add(rootNode);
                }
            }
        }
        // Second pass: link children for all nodes
        for (WorkflowNodeExecution node : this.nodeExecutionMap.values()) {
            node.getChildren().clear();
            if (node.getChildrenIds() != null) {
                for (String childId : node.getChildrenIds()) {
                    WorkflowNodeExecution child = this.nodeExecutionMap.get(childId);
                    if (child != null) {
                        node.getChildren().add(child);
                    }
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<WorkflowNodeExecution> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowNodeExecution> nodes) {
        this.nodes = nodes;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecutionStatus status) {
        this.status = status;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Map<String, WorkflowNodeExecution> getNodeExecutionMap() {
        return nodeExecutionMap;
    }

    public void setNodeExecutionMap(Map<String, WorkflowNodeExecution> nodeExecutionMap) {
        this.nodeExecutionMap = nodeExecutionMap;
    }

    public List<String> getRootNodeExecutionIds() {
        return rootNodeExecutionIds;
    }

    public void setRootNodeExecutionIds(List<String> rootNodeExecutionIds) {
        this.rootNodeExecutionIds = rootNodeExecutionIds;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }
}
