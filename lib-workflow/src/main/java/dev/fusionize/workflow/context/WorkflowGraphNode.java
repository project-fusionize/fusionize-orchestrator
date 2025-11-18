package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;

import java.util.ArrayList;
import java.util.List;

public class WorkflowGraphNode {
    private String node;
    private WorkflowNodeExecutionState state = WorkflowNodeExecutionState.IDLE;
    private List<String> parents = new ArrayList<>();

    public WorkflowGraphNode renew() {
        WorkflowGraphNode copy = new WorkflowGraphNode();
        copy.node = this.node;
        copy.state = this.state;
        copy.parents = new ArrayList<>(this.parents);
        return copy;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public WorkflowNodeExecutionState getState() {
        return state;
    }

    public void setState(WorkflowNodeExecutionState state) {
        this.state = state;
    }

    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    @Override
    public String toString() {
        return "WorkflowGraphNode{" +
                "node='" + node + '\'' +
                ", state=" + state +
                ", parents=" + parents +
                '}';
    }
}
