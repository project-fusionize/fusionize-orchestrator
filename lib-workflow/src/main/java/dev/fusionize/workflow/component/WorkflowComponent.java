package dev.fusionize.workflow.component;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import dev.fusionize.workflow.WorkflowNodeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow-component")
public class WorkflowComponent extends DomainEntity {
    @Id
    private String id;
    @Indexed(unique = true)
    private String componentId;
    private String description;
    private WorkflowNodeType compatible;

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private String componentId;
        private String description;
        private WorkflowNodeType compatible;

        private Builder(String parentDomain) {
            super(parentDomain, KeyUtil.getFlatUUID());
        }

        public Builder withComponentId(String componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCompatible(WorkflowNodeType compatible) {
            this.compatible = compatible;
            return this;
        }

        public WorkflowComponent build() {
            WorkflowComponent component = new WorkflowComponent();
            component.load(super.build());
            component.setComponentId(this.componentId);
            component.setDescription(this.description);
            component.setCompatible(this.compatible);
            return component;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkflowNodeType getCompatible() {
        return compatible;
    }

    public void setCompatible(WorkflowNodeType compatible) {
        this.compatible = compatible;
    }
}
