package dev.fusionize.workflow;

import java.util.concurrent.ConcurrentHashMap;

public class WorkflowContext {
    private ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Object> getContext() {
        return context;
    }

    public void setContext(ConcurrentHashMap<String, Object> context) {
        this.context = context;
    }
}
