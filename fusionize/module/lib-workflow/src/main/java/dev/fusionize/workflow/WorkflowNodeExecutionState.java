package dev.fusionize.workflow;

import java.util.HashMap;
import java.util.Map;

public enum WorkflowNodeExecutionState {
    IDLE("IDLE"),
    WORKING("WORKING"),
    WAITING("WAITING"),
    DONE("DONE");

    private final String name;

    WorkflowNodeExecutionState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, WorkflowNodeExecutionState> lookup = new HashMap<>();

    static{
        for(WorkflowNodeExecutionState state : WorkflowNodeExecutionState.values())
            lookup.put(state.getName(), state);
    }
    public static WorkflowNodeExecutionState get(String name){
        return lookup.get(name);
    }
}
