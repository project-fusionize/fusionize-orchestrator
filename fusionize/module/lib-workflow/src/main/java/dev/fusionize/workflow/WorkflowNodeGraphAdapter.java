package dev.fusionize.workflow;

import dev.fusionize.common.graph.NodeAdapter;

import java.util.ArrayList;
import java.util.Collection;

public class WorkflowNodeGraphAdapter implements NodeAdapter<WorkflowNode, String> {
    public String getId(WorkflowNode n) {
        return n.getWorkflowNodeId();
    }

    public Collection<WorkflowNode> getChildren(WorkflowNode n) {
        return n.getChildren();
    }

    public void setChildren(WorkflowNode n, Collection<WorkflowNode> c) {
        n.setChildren(new ArrayList<>(c));
    }

    public Collection<String> getChildrenIds(WorkflowNode n) {
        return n.getChildrenIds();
    }

    public void setChildrenIds(WorkflowNode n, Collection<String> ids) {
        n.setChildrenIds(new ArrayList<>(ids));
    }
}
