package dev.fusionize.workflow.descriptor;

import dev.fusionize.common.parser.descriptor.Description;
import dev.fusionize.workflow.WorkflowNodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowNodeDescription extends Description {
    private WorkflowNodeType type;
    private String actor;
    private String component;
    private Map<String, Object> config = new HashMap<>();
    private List<String> next = new ArrayList<>();

    public WorkflowNodeDescription() {
    }

    public WorkflowNodeType getType() {
        return type;
    }

    public void setType(WorkflowNodeType type) {
        this.type = type;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public List<String> getNext() {
        return next;
    }

    public void setNext(List<String> next) {
        this.next = next;
    }
}
