package dev.fusionize.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties({"nodes"})
@Document(collection = "workflow")
public class Workflow extends DomainEntity {
    @Id
    private String id;
    @Indexed(unique = true)
    private String workflowId;
    private String description;
    private int version;
    private boolean active = true;
    private Map<String, WorkflowNode> nodeMap = new java.util.HashMap<>();
    private List<String> rootNodeIds = new ArrayList<>();
    @Transient
    private List<WorkflowNode> nodes = new ArrayList<>();

    public WorkflowNode findNode(String workflowNodeId) {
        if (nodeMap.containsKey(workflowNodeId)) {
            return nodeMap.get(workflowNodeId);
        }
        return nodes.stream().filter(n-> n.getWorkflowNodeId().equals(workflowNodeId)).findFirst().orElse(
                nodes.stream().map(n->n.findNode(workflowNodeId)).filter(Objects::nonNull).findFirst().orElse(null)
        );
    }

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public void mergeFrom(Workflow other) {
        if (other == null) return;
        if (other.getName() != null) this.setName(other.getName());
        if (other.getDomain() != null) this.setDomain(other.getDomain());
        if (other.getDescription() != null) this.setDescription(other.getDescription());
        if (other.getVersion() != 0) this.setVersion(other.getVersion());
        this.setActive(other.isActive());

        if (other.getNodes() != null && !other.getNodes().isEmpty()) {
            // Ensure this workflow is flat so we have a full nodeMap
            this.flatten();
            
            // Build map of existing nodes keyed by WorkflowNodeKey for matching logic
            Map<String, WorkflowNode> existingNodesByKey = new java.util.HashMap<>();
            for (WorkflowNode node : this.nodeMap.values()) {
                if (node.getWorkflowNodeKey() != null) {
                    existingNodesByKey.put(node.getWorkflowNodeKey(), node);
                }
            }

            // Process incoming nodes
            updateNodeIds(other.getNodes(), existingNodesByKey);
            this.setNodes(other.getNodes());
            
            // Re-flatten to keep internal state consistent after setting new nodes
            this.flatten();
        }
    }

    public void flatten() {
        this.nodeMap.clear();
        this.rootNodeIds.clear();
        if (this.nodes != null) {
            for (WorkflowNode node : this.nodes) {
                this.rootNodeIds.add(node.getWorkflowNodeId());
                flattenNode(node, this.nodeMap);
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

    public void inflate() {
        this.nodes.clear();
        // First pass: link roots
        if (this.rootNodeIds != null) {
            for (String rootId : this.rootNodeIds) {
                WorkflowNode rootNode = this.nodeMap.get(rootId);
                if (rootNode != null) {
                    this.nodes.add(rootNode);
                }
            }
        }
        // Second pass: link children for all nodes
        for (WorkflowNode node : this.nodeMap.values()) {
            node.getChildren().clear();
            if (node.getChildrenIds() != null) {
                for (String childId : node.getChildrenIds()) {
                    WorkflowNode child = this.nodeMap.get(childId);
                    if (child != null) {
                        node.getChildren().add(child);
                    }
                }
            }
        }
    }



    private void updateNodeIds(List<WorkflowNode> incomingNodes, Map<String, WorkflowNode> existingNodes) {
        if (incomingNodes == null) return;
        for (WorkflowNode incoming : incomingNodes) {
            if (incoming.getWorkflowNodeKey() != null && existingNodes.containsKey(incoming.getWorkflowNodeKey())) {
                WorkflowNode existing = existingNodes.get(incoming.getWorkflowNodeKey());
                if (Objects.equals(existing, incoming)) {
                    // Content is identical, preserve the ID
                    incoming.setWorkflowNodeId(existing.getWorkflowNodeId());
                }
            }
            // Recurse for children
            if (incoming.getChildren() != null) {
                updateNodeIds(incoming.getChildren(), existingNodes);
            }
        }
    }


    public static class Builder extends DomainEntity.Builder<Builder> {
        private String workflowId;
        private String description;
        private int version;
        private boolean active = true;
        private List<WorkflowNode> nodes = new ArrayList<>();

        private Builder(String parentDomain) {
            super(parentDomain, KeyUtil.getFlatUUID());
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withVersion(int version) {
            this.version = version;
            return this;
        }

        public Builder withWorkflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder withNodes(List<WorkflowNode> nodes) {
            this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
            return this;
        }

        public Builder addNode(WorkflowNode node) {
            if (node != null) {
                this.nodes.add(node);
            }
            return this;
        }

        public Builder addNode(WorkflowNode.Builder nodeBuilder) {
            if (nodeBuilder != null) {
                this.nodes.add(nodeBuilder.build());
            }
            return this;
        }

        public Builder addNodes(List<WorkflowNode> nodes) {
            if (nodes != null) {
                this.nodes.addAll(nodes);
            }
            return this;
        }

        public Builder addNodes(WorkflowNode... nodes) {
            if (nodes != null) {
                for (WorkflowNode node : nodes) {
                    if (node != null) {
                        this.nodes.add(node);
                    }
                }
            }
            return this;
        }

        public Workflow build() {
            Workflow workflow = new Workflow();
            workflow.load(super.build());
            workflow.setDescription(this.description);
            workflow.setVersion(this.version);
            if(this.workflowId!=null){
                workflow.setWorkflowId(this.workflowId);
            }else{
                workflow.setWorkflowId(KeyUtil.getTimestampId("WFLO"));
            }
            workflow.setActive(this.active);
            workflow.setNodes(new ArrayList<>(this.nodes));
            return workflow;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<WorkflowNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public Map<String, WorkflowNode> getNodeMap() {
        return nodeMap;
    }

    public void setNodeMap(Map<String, WorkflowNode> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public List<String> getRootNodeIds() {
        return rootNodeIds;
    }

    public void setRootNodeIds(List<String> rootNodeIds) {
        this.rootNodeIds = rootNodeIds;
    }
}