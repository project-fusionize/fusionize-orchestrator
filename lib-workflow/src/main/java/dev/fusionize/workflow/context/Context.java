package dev.fusionize.workflow.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class Context {
    private ConcurrentHashMap<String, Object> data;
    private List<WorkflowDecision> decisions;
    private List<WorkflowGraphNode> graphNodes;

    public Context() {
        this.data = new ConcurrentHashMap<>();
        this.decisions = new ArrayList<>();
        this.graphNodes = new ArrayList<>();
    }

    public Context renew() {
        Context copy = new Context();
        copy.data = new ConcurrentHashMap<>(this.data);
        if (this.decisions != null) {
            List<WorkflowDecision> copiedDecisions = new ArrayList<>();
            for (WorkflowDecision decision : this.decisions) {
                copiedDecisions.add(decision != null ? decision.renew() : null);
            }
            copy.decisions = copiedDecisions;
        }
        if (this.graphNodes != null) {
            List<WorkflowGraphNode> copiedCurrentNodes = new ArrayList<>();
            for (WorkflowGraphNode node : this.graphNodes) {
                copiedCurrentNodes.add(node != null ? node.renew() : null);
            }
            copy.graphNodes = copiedCurrentNodes;
        }
        return copy;
    }

    public static class Builder {
        private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
        private final List<WorkflowDecision> decisions = new ArrayList<>();
        private final List<WorkflowGraphNode> graphNodes = new ArrayList<>();

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

        public Builder graphNodes(WorkflowGraphNode... graphNodes) {
            this.graphNodes.addAll(Arrays.stream(graphNodes).toList());
            return this;
        }

        public Context build() {
            Context ctx = new Context();
            ctx.data = new ConcurrentHashMap<>(this.data);
            ctx.decisions = new ArrayList<>(this.decisions);
            ctx.graphNodes = new ArrayList<>(this.graphNodes);
            return ctx;
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

    public List<WorkflowGraphNode> getGraphNodes() {
        return graphNodes;
    }

    public void setGraphNodes(List<WorkflowGraphNode> graphNodes) {
        this.graphNodes = graphNodes;
    }

    public List<WorkflowGraphNodeRecursive> currentNodes() {
        return ContextUtility.extractCurrentNodes(this);
    }

    public WorkflowDecision latestDecisionForNode(String nodeKey) {
        return ContextUtility.getLatestDecisionForNode(this, nodeKey);
    }

    public <T> Optional<T> var(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Optional<String> varString(String key) {
        return var(key, String.class);
    }

    public Optional<Integer> varInt(String key) {
        return var(key, Integer.class);
    }

    public Optional<Double> varDouble(String key) {
        return var(key, Double.class);
    }

    public Optional<Float> varFloat(String key) {
        return var(key, Float.class);
    }

    @SuppressWarnings("rawtypes")
    public Optional<List> varList(String key) {
        return var(key, List.class);
    }

    @SuppressWarnings("rawtypes")
    public Optional<Map> varMap(String key) {
        return var(key, Map.class);
    }

    @Override
    public String toString() {
        return "Context{" +
                "data=" + data +
                ", decisions=" + decisions +
                ", graphNodes=" + graphNodes +
                '}';
    }
}
