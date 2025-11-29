package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;

public class ContextRuntimeData {
    private String workflowId;
    private String workflowExecutionId;
    private String workflowNodeId;
    private String workflowNodeKey;
    private String workflowNodeExecutionId;
    private RuntimeData runtimeData;
    public record RuntimeData(WorkflowExecution workflowExecution, WorkflowNodeExecution nodeExecution){}

    public static ContextRuntimeData from(WorkflowExecution workflowExecution, WorkflowNodeExecution nodeExecution) {
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.workflowExecutionId = workflowExecution.getWorkflowExecutionId();
        runtimeData.workflowId = workflowExecution.getWorkflowId();
        runtimeData.workflowNodeId = nodeExecution.getWorkflowNodeId();
        runtimeData.workflowNodeKey = nodeExecution.getWorkflowNode().getWorkflowNodeKey();
        runtimeData.workflowNodeExecutionId = nodeExecution.getWorkflowNodeExecutionId();
        runtimeData.runtimeData = new RuntimeData(workflowExecution, nodeExecution);
        return runtimeData;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(String workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

    public String getWorkflowNodeKey() {
        return workflowNodeKey;
    }

    public void setWorkflowNodeKey(String workflowNodeKey) {
        this.workflowNodeKey = workflowNodeKey;
    }

    public String getWorkflowNodeExecutionId() {
        return workflowNodeExecutionId;
    }

    public void setWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        this.workflowNodeExecutionId = workflowNodeExecutionId;
    }

    public RuntimeData getRuntimeData() {
        return runtimeData;
    }

    public void setRuntimeData(RuntimeData runtimeData) {
        this.runtimeData = runtimeData;
    }
}
