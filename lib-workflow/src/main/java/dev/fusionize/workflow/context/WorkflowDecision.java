package dev.fusionize.workflow.context;

import java.util.HashMap;
import java.util.Map;

public class WorkflowDecision {
    private String decisionNode;
    private Map<String, Boolean> optionNodes = new HashMap<>();

    public WorkflowDecision renew() {
        WorkflowDecision copy = new WorkflowDecision();
        copy.decisionNode = this.decisionNode;
        copy.optionNodes = new HashMap<>(this.optionNodes);
        return copy;
    }

    public String getDecisionNode() {
        return decisionNode;
    }

    public void setDecisionNode(String decisionNode) {
        this.decisionNode = decisionNode;
    }

    public Map<String, Boolean> getOptionNodes() {
        return optionNodes;
    }

    public void setOptionNodes(Map<String, Boolean> optionNodes) {
        this.optionNodes = optionNodes;
    }

    @Override
    public String toString() {
        return "WorkflowDecision{" +
                "decisionNode='" + decisionNode + '\'' +
                ", optionNodes=" + optionNodes +
                '}';
    }
}
