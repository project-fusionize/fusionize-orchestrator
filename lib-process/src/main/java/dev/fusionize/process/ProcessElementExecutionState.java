package dev.fusionize.process;

import java.util.HashMap;
import java.util.Map;

public enum ProcessElementExecutionState {
    IDLE("IDLE"),
    ACTIVE("ACTIVE"),
    WAITING("WAITING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    SKIPPED("SKIPPED");

    private final String name;

    ProcessElementExecutionState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, ProcessElementExecutionState> lookup = new HashMap<>();

    static {
        for (ProcessElementExecutionState state : ProcessElementExecutionState.values()) {
            lookup.put(state.getName(), state);
        }
    }

    public static ProcessElementExecutionState get(String name) {
        return lookup.get(name);
    }
}

