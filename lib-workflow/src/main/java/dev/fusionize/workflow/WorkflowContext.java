package dev.fusionize.workflow;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class WorkflowContext {
    private ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

    // Default constructor
    public WorkflowContext() {
        this.context = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<String, Object> getContext() {
        return context;
    }

    public void setContext(ConcurrentHashMap<String, Object> context) {
        this.context = context;
    }

    // Builder pattern implementation
    public static class Builder {
        private ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

        public Builder() {
        }

        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder addAllContext(Map<String, Object> contextMap) {
            this.context.putAll(contextMap);
            return this;
        }

        public Builder context(ConcurrentHashMap<String, Object> context) {
            this.context = new ConcurrentHashMap<>(context);
            return this;
        }

        public WorkflowContext build() {
            WorkflowContext workflowContext = new WorkflowContext();
            workflowContext.context = new ConcurrentHashMap<>(this.context);
            return workflowContext;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "WorkflowContext{" +
                "context=" + context +
                '}';
    }
}
