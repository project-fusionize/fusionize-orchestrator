package dev.fusionize.workflow;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.context.Context;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkflowNodeExecution {
    private List<WorkflowNodeExecution> children = new ArrayList<>();
    private String workflowNodeId;
    private String workflowNodeExecutionId;
    private WorkflowNodeExecutionState state;
    private Context stageContext;
    @Transient
    private WorkflowNode workflowNode;

    public static WorkflowNodeExecution of(WorkflowNode node, Context context) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.workflowNodeExecutionId = KeyUtil.getTimestampId("NEXE");
        execution.workflowNodeId = node.getWorkflowNodeId();
        execution.state = WorkflowNodeExecutionState.IDLE;
        execution.stageContext = context;
        execution.workflowNode = node;
        return execution;
    }

    public WorkflowNodeExecution findNodeByWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        return children.stream()
                .filter(n -> n.getWorkflowNodeExecutionId().equals(workflowNodeExecutionId))
                .findFirst()
                .orElseGet(() -> children.stream()
                        .map(n -> n.findNodeByWorkflowNodeExecutionId(workflowNodeExecutionId))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    public List<WorkflowNodeExecution> findNodesByWorkflowNodeId(String workflowNodeId) {
        List<WorkflowNodeExecution> nodesByWorkflowNodeId =  children.stream()
                .filter(n -> n.getWorkflowNodeId().equals(workflowNodeId)).toList();
        List<WorkflowNodeExecution> childrenNodesByWorkflowNodeId = children.stream()
                .map(n -> n.findNodesByWorkflowNodeId(workflowNodeId))
                .flatMap(Collection::stream).toList();
        return Stream.concat(nodesByWorkflowNodeId.stream(), childrenNodesByWorkflowNodeId.stream())
                .collect(Collectors.toList());
    }


    public WorkflowNodeExecution renew() {
        WorkflowNodeExecution clone = new WorkflowNodeExecution();
        clone.workflowNodeExecutionId = KeyUtil.getTimestampId("NEXE"); // new ID
        clone.workflowNodeId = this.workflowNodeId;
        clone.state = WorkflowNodeExecutionState.IDLE;
        clone.stageContext = this.stageContext != null ? this.stageContext.renew() : null;
        clone.workflowNode = this.workflowNode;

        List<WorkflowNodeExecution> renewedChildren = new ArrayList<>();
        for (WorkflowNodeExecution child : this.children) {
            renewedChildren.add(child.renew());
        }
        clone.children = renewedChildren;

        return clone;
    }

    public List<WorkflowNodeExecution> getChildren() {
        return children;
    }

    public void setChildren(List<WorkflowNodeExecution> children) {
        this.children = children;
    }

    public String getWorkflowNodeExecutionId() {
        return workflowNodeExecutionId;
    }

    public void setWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        this.workflowNodeExecutionId = workflowNodeExecutionId;
    }

    public String getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(String workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

    public WorkflowNodeExecutionState getState() {
        return state;
    }

    public void setState(WorkflowNodeExecutionState state) {
        this.state = state;
    }

    public Context getStageContext() {
        return stageContext;
    }

    public void setStageContext(Context stageContext) {
        this.stageContext = stageContext;
    }

    public WorkflowNode getWorkflowNode() {
        return workflowNode;
    }

    public void setWorkflowNode(WorkflowNode workflowNode) {
        this.workflowNode = workflowNode;
    }
}
