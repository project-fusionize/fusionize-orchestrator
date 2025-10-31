package dev.fusionize.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowContext {
    private ConcurrentHashMap<String, Object> data;
    private List<WorkflowDecision> decisions;

    public WorkflowContext() {
        this.data = new ConcurrentHashMap<>();
        this.decisions = new ArrayList<>();
    }

    public WorkflowContext renew() {
        WorkflowContext copy = new WorkflowContext();
        copy.data = new ConcurrentHashMap<>(this.data);
        if (this.decisions != null) {
            List<WorkflowDecision> copiedDecisions = new ArrayList<>();
            for (WorkflowDecision decision : this.decisions) {
                copiedDecisions.add(decision != null ? decision.renew() : null);
            }
            copy.decisions = copiedDecisions;
        }
        return copy;
    }

    public static class Builder {
        private ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
        private List<WorkflowDecision> decisions = new ArrayList<>();

        public Builder() {
        }

        public Builder add(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public Builder addAll(Map<String, Object> contextMap) {
            this.data.putAll(contextMap);
            return this;
        }

        public Builder decisions(WorkflowDecision... decisions) {
            this.decisions.addAll(Arrays.stream(decisions).toList());
            return this;
        }

        public WorkflowContext build() {
            WorkflowContext workflowContext = new WorkflowContext();
            workflowContext.data = new ConcurrentHashMap<>(this.data);
            return workflowContext;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConcurrentHashMap<String, Object> getData() {
        return data;
    }

    public void setData(ConcurrentHashMap<String, Object> data) {
        this.data = data;
    }


    public List<WorkflowDecision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<WorkflowDecision> decisions) {
        this.decisions = decisions;
    }

    @Override
    public String toString() {
        return "WorkflowContext{" +
                "data=" + data +
                ", decisions=" + decisions +
                '}';
    }
}
