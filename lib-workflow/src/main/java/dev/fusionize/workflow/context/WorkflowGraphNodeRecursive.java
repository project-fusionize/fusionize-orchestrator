package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;

import java.util.ArrayList;
import java.util.List;

public class WorkflowGraphNodeRecursive {
    private String node;
    private WorkflowNodeExecutionState state;
    private List<WorkflowGraphNodeRecursive> parents = new ArrayList<>();
    private List<WorkflowGraphNodeRecursive> children = new ArrayList<>();

    public WorkflowGraphNodeRecursive(String node, WorkflowNodeExecutionState state) {
        this.node = node;
        this.state = state;
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

    public List<WorkflowGraphNodeRecursive> getParents() {
        return parents;
    }

    public void setParents(List<WorkflowGraphNodeRecursive> parents) {
        this.parents = parents;
    }

    public List<WorkflowGraphNodeRecursive> getChildren() {
        return children;
    }

    public void setChildren(List<WorkflowGraphNodeRecursive> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "WorkflowGraphNodeRecursive{" +
                "node='" + node + '\'' +
                ", state=" + state +
                ", parents=" + parents.stream().map(WorkflowGraphNodeRecursive::getNode).toList() +
                ", children=" + children.stream().map(WorkflowGraphNodeRecursive::getNode).toList() +
                '}';
    }
}
