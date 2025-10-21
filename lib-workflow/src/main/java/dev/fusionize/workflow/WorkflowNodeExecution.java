package dev.fusionize.workflow;

import dev.fusionize.workflow.component.WorkflowComponentRuntime;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;

public class WorkflowNodeExecution {
    private List<WorkflowNodeExecution> children = new ArrayList<>();
    private String workflowNodeId;
    private WorkflowNodeExecutionState state;
    private WorkflowContext stageContext;
    @Transient
    private WorkflowNode workflowNode;
    @Transient
    private WorkflowComponentRuntime runtime;

    public static WorkflowNodeExecution of(WorkflowNode node, WorkflowContext context) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.workflowNodeId = node.getWorkflowNodeId();
        execution.state = WorkflowNodeExecutionState.IDLE;
        execution.stageContext = context;
        execution.workflowNode = node;
        return execution;
    }

    public List<WorkflowNodeExecution> getChildren() {
        return children;
    }

    public void setChildren(List<WorkflowNodeExecution> children) {
        this.children = children;
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

    public WorkflowContext getStageContext() {
        return stageContext;
    }

    public void setStageContext(WorkflowContext stageContext) {
        this.stageContext = stageContext;
    }

    public WorkflowNode getWorkflowNode() {
        return workflowNode;
    }

    public void setWorkflowNode(WorkflowNode workflowNode) {
        this.workflowNode = workflowNode;
    }

    public WorkflowComponentRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(WorkflowComponentRuntime runtime) {
        this.runtime = runtime;
    }
}
