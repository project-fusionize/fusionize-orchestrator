package dev.fusionize.workflow;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.component.WorkflowComponentConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorkflowNode {
    private List<WorkflowNode> children = new ArrayList<>();
    private WorkflowNodeType type;
    private String workflowNodeId;
    private String workflowNodeKey;
    private String component;
    private WorkflowComponentConfig componentConfig;

    public WorkflowNode(){}

    public WorkflowNode findNode(String workflowNodeId) {
        return children.stream().filter(n-> n.getWorkflowNodeId().equals(workflowNodeId)).findFirst().orElse(
                children.stream().map(n->n.findNode(workflowNodeId)).filter(Objects::nonNull).findFirst().orElse(null)
        );
    }


    private WorkflowNode(Builder builder) {
        this.children = builder.children;
        this.type = builder.type;
        if(builder.workflowNodeId != null) {
            this.workflowNodeId = builder.workflowNodeId;
        }else{
            this.workflowNodeId = KeyUtil.getTimestampId("WNOD");
        }
        this.workflowNodeKey = builder.workflowNodeKey;
        this.component = builder.component;
        this.componentConfig = builder.componentConfig;
    }

    public List<WorkflowNode> getChildren() {
        return children;
    }

    public void setChildren(List<WorkflowNode> children) {
        this.children = children;
    }

    public WorkflowNodeType getType() {
        return type;
    }

    public void setType(WorkflowNodeType type) {
        this.type = type;
    }

    public String getWorkflowNodeKey() {
        return workflowNodeKey;
    }

    public void setWorkflowNodeKey(String workflowNodeKey) {
        this.workflowNodeKey = workflowNodeKey;
    }

    public String getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(String workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public WorkflowComponentConfig getComponentConfig() {
        return componentConfig;
    }

    public void setComponentConfig(WorkflowComponentConfig componentConfig) {
        this.componentConfig = componentConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<WorkflowNode> children = new ArrayList<>();
        private WorkflowNodeType type;
        private String workflowNodeId;
        private String workflowNodeKey;
        private String component;
        private WorkflowComponentConfig componentConfig;

        public Builder children(List<WorkflowNode> children) {
            this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
            return this;
        }

        public Builder addChild(WorkflowNode child) {
            if (child != null) {
                this.children.add(child);
            }
            return this;
        }

        public Builder type(WorkflowNodeType type) {
            this.type = type;
            return this;
        }

        public Builder workflowNodeId(String workflowNodeId) {
            this.workflowNodeId = workflowNodeId;
            return this;
        }

        public Builder workflowNodeKey(String workflowNodeKey) {
            this.workflowNodeKey = workflowNodeKey;
            return this;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder componentConfig(WorkflowComponentConfig componentConfig) {
            this.componentConfig = componentConfig;
            return this;
        }

        public WorkflowNode build() {
            return new WorkflowNode(this);
        }
    }
}
