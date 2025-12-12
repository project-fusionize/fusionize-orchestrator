package dev.fusionize.workflow;

import java.util.HashMap;
import java.util.Map;

public enum WorkflowNodeType {
    START("START"),
    DECISION("DECISION"),
    TASK("TASK"),
    WAIT("WAIT"),
    END("END");

    private final String name;

    WorkflowNodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, WorkflowNodeType> lookup = new HashMap<>();

    static{
        for(WorkflowNodeType type : WorkflowNodeType.values())
            lookup.put(type.getName(), type);
    }
    public static WorkflowNodeType get(String name){
        return lookup.get(name);
    }
}
