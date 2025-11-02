package dev.fusionize.workflow.descriptor;

import dev.fusionize.common.parser.descriptor.Description;

import java.util.HashMap;
import java.util.Map;

public class WorkflowDescription extends Description {
    private String name;
    private String domain;
    private String key;
    private String description;
    private int version;
    private boolean active = true;
    private Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
    public WorkflowDescription() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public Map<String, WorkflowNodeDescription> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, WorkflowNodeDescription> nodes) {
        this.nodes = nodes;
    }
}
