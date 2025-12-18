package dev.fusionize.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.fusionize.common.graph.FlattenResult;
import dev.fusionize.common.graph.GraphUtil;
import dev.fusionize.common.graph.NodeAdapter;
import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@JsonIgnoreProperties({"nodes"})
@Document(collection = "workflow")
public class Workflow extends DomainEntity {
    private static final NodeAdapter<WorkflowNode, String> workflowAdapter =
            new WorkflowNodeGraphAdapter();
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



    public void flatten() {
        this.nodeMap.clear();
        this.rootNodeIds.clear();
        FlattenResult<WorkflowNode, String> result =
                GraphUtil.flatten(this.getNodes(), workflowAdapter);

        this.setNodeMap(result.nodeMap());
        this.setRootNodeIds(new ArrayList<>(result.rootIds()));
    }

    public void inflate() {
        this.nodes.clear();
        Collection<WorkflowNode> roots =
                GraphUtil.inflate(
                        this.getNodeMap(),
                        this.getRootNodeIds(),
                        workflowAdapter
                );

        this.setNodes(new ArrayList<>(roots));
    }


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