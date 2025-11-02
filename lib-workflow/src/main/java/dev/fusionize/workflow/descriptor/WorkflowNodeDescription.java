package dev.fusionize.workflow.descriptor;

import dev.fusionize.common.parser.descriptor.Description;
import dev.fusionize.workflow.WorkflowNodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowNodeDescription extends Description {
    private WorkflowNodeType type;
    private String component;
    private Map<String,Object> componentConfig = new HashMap<>();
    private List<String> next = new ArrayList<>();

    public WorkflowNodeDescription() {}

    public WorkflowNodeType getType() {
        return type;
    }

    public void setType(WorkflowNodeType type) {
        this.type = type;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Map<String, Object> getComponentConfig() {
        return componentConfig;
    }

    public void setComponentConfig(Map<String, Object> componentConfig) {
        this.componentConfig = componentConfig;
    }

    public List<String> getNext() {
        return next;
    }

    public void setNext(List<String> next) {
        this.next = next;
    }
}
