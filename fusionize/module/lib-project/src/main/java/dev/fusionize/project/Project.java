package dev.fusionize.project;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "project")
public class Project extends DomainEntity {
    @Id
    private String id;
    private String description;

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private String description;
        private Builder(String parentDomain) {
            super(parentDomain, KeyUtil.getFlatUUID());
        }


        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Project build() {
            Project project = new Project();
            project.load(super.build());
            project.setDescription(this.description);
            return project;
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) && Objects.equals(description, project.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, description);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                "} " + super.toString();
    }
}