package dev.fusionize.workflow;

import dev.fusionize.common.graph.NodeAdapter;

import java.util.ArrayList;
import java.util.Collection;

public class WorkflowNodeExecutionGraphAdapter implements NodeAdapter<WorkflowNodeExecution, String> {
    public String getId(WorkflowNodeExecution n) {
        return n.getWorkflowNodeExecutionId();
    }

    public Collection<WorkflowNodeExecution> getChildren(WorkflowNodeExecution n) {
        return n.getChildren();
    }

    public void setChildren(WorkflowNodeExecution n, Collection<WorkflowNodeExecution> c) {
        n.setChildren(new ArrayList<>(c));
    }

    public Collection<String> getChildrenIds(WorkflowNodeExecution n) {
        return n.getChildrenIds();
    }

    public void setChildrenIds(WorkflowNodeExecution n, Collection<String> ids) {
        n.setChildrenIds(new ArrayList<>(ids));
    }
}
