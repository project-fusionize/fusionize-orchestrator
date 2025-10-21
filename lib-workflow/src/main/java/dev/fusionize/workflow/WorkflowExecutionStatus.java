package dev.fusionize.workflow;

import java.util.HashMap;
import java.util.Map;

public enum WorkflowExecutionStatus {
    IN_PROGRESS("IN_PROGRESS"),
    ERROR("ERROR"),
    SUCCESS("SUCCESS"),
    TERMINATED("TERMINATED");

    private final String name;

    WorkflowExecutionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, WorkflowExecutionStatus> lookup = new HashMap<>();

    static{
        for(WorkflowExecutionStatus status : WorkflowExecutionStatus.values())
            lookup.put(status.getName(), status);
    }
    public static WorkflowExecutionStatus get(String name){
        return lookup.get(name);
    }
}
