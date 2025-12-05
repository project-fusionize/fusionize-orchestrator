package dev.fusionize.workflow.component;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;

import java.util.HashSet;
import java.util.Set;

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
    private Set<Actor> actors = new HashSet<>();

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public void mergeFrom(WorkflowComponent other) {
        if (other == null)
            return;
        if (other.getName() != null)
            this.setName(other.getName());
        if (other.getDescription() != null)
            this.setDescription(other.getDescription());
        if (other.getActors() != null && !other.getActors().isEmpty())
            this.setActors(other.getActors());
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private String componentId;
        private String description;
        private Set<Actor> actors = new HashSet<>();

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

        public Builder withActor(Actor actor) {
            this.actors.add(actor);
            return this;
        }

        public Builder withActors(Set<Actor> actors) {
            this.actors.addAll(actors);
            return this;
        }

        public WorkflowComponent build() {
            WorkflowComponent component = new WorkflowComponent();
            component.load(super.build());
            if (componentId == null) {
                component.setComponentId(KeyUtil.getTimestampId("COMP"));
            } else {
                component.setComponentId(this.componentId);
            }
            component.setDescription(this.description);
            component.setActors(this.actors);
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

    public Set<Actor> getActors() {
        return actors;
    }

    public void setActors(Set<Actor> actors) {
        this.actors = actors;
    }
}
