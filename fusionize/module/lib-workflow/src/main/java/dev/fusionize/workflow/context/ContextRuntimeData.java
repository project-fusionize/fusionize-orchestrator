package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;

public class ContextRuntimeData {
    private String workflowId;
    private String workflowDomain;
    private String workflowExecutionId;
    private String workflowNodeId;
    private String workflowNodeKey;
    private String workflowNodeExecutionId;
    private ExecutionData executionData;
    public record ExecutionData(WorkflowExecution workflowExecution, WorkflowNodeExecution nodeExecution){}

    public static ContextRuntimeData from(WorkflowExecution workflowExecution, WorkflowNodeExecution nodeExecution) {
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.workflowExecutionId = workflowExecution.getWorkflowExecutionId();
        runtimeData.workflowId = workflowExecution.getWorkflowId();
        if(workflowExecution.getWorkflow()!=null){
            runtimeData.workflowDomain = workflowExecution.getWorkflow().getDomain();
        }
        runtimeData.workflowNodeId = nodeExecution.getWorkflowNodeId();
        if(nodeExecution.getWorkflowNode()!=null){
            runtimeData.workflowNodeKey = nodeExecution.getWorkflowNode().getWorkflowNodeKey();
        }
        runtimeData.workflowNodeExecutionId = nodeExecution.getWorkflowNodeExecutionId();
        runtimeData.executionData = new ExecutionData(workflowExecution, nodeExecution);
        return runtimeData;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowDomain() {
        return workflowDomain;
    }

    public void setWorkflowDomain(String workflowDomain) {
        this.workflowDomain = workflowDomain;
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

    public ExecutionData getExecutionData() {
        return executionData;
    }

    public void setExecutionData(ExecutionData executionData) {
        this.executionData = executionData;
    }
}
