package dev.fusionize.workflow;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.component.runtime.ComponentRuntime;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;

public class WorkflowNodeExecution {
    private List<WorkflowNodeExecution> children = new ArrayList<>();
    private String workflowNodeId;
    private String workflowNodeExecutionId;
    private WorkflowNodeExecutionState state;
    private WorkflowContext stageContext;
    @Transient
    private WorkflowNode workflowNode;
    @Transient
    private ComponentRuntime runtime;

    public static WorkflowNodeExecution of(WorkflowNode node, WorkflowContext context) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.workflowNodeExecutionId = KeyUtil.getTimestampId("NEXE");
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

    public ComponentRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(ComponentRuntime runtime) {
        this.runtime = runtime;
    }
}
