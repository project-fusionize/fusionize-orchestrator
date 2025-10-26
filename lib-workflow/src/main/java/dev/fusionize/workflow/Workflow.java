package dev.fusionize.workflow;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "workflow")
public class Workflow extends DomainEntity {
    @Id
    private String id;
    @Indexed(unique = true)
    private String workflowId;
    private String description;
    private int version;
    private boolean active = true;
    private List<WorkflowNode> nodes = new ArrayList<>();

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
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
}