package dev.fusionize.process;

import java.util.HashMap;
import java.util.Map;

public enum ProcessExecutionStatus {
    IDLE("IDLE"),
    IN_PROGRESS("IN_PROGRESS"),
    ERROR("ERROR"),
    SUCCESS("SUCCESS"),
    TERMINATED("TERMINATED"),
    SUSPENDED("SUSPENDED");

    private final String name;

    ProcessExecutionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, ProcessExecutionStatus> lookup = new HashMap<>();

    static {
        for (ProcessExecutionStatus status : ProcessExecutionStatus.values()) {
            lookup.put(status.getName(), status);
        }
    }

    public static ProcessExecutionStatus get(String name) {
        return lookup.get(name);
    }
}

