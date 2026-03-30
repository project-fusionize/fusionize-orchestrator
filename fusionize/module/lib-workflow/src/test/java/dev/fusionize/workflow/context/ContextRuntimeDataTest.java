package dev.fusionize.workflow.context;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextRuntimeDataTest {

    @Test
    void shouldCreateFromExecutionAndNode() {
        // setup
        var workflow = new Workflow();
        workflow.setDomain("test-domain");

        var workflowNode = WorkflowNode.builder()
                .workflowNodeKey("node-key-1")
                .type(WorkflowNodeType.TASK)
                .build();

        var workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowExecutionId("exec-1");
        workflowExecution.setWorkflowId("wf-1");
        workflowExecution.setWorkflow(workflow);

        var nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNodeId("node-1");
        nodeExecution.setWorkflowNodeExecutionId("nexec-1");
        nodeExecution.setWorkflowNode(workflowNode);

        // expectation
        var runtimeData = ContextRuntimeData.from(workflowExecution, nodeExecution);

        // validation
        assertThat(runtimeData.getWorkflowExecutionId()).isEqualTo("exec-1");
        assertThat(runtimeData.getWorkflowId()).isEqualTo("wf-1");
        assertThat(runtimeData.getWorkflowDomain()).isEqualTo("test-domain");
        assertThat(runtimeData.getWorkflowNodeId()).isEqualTo("node-1");
        assertThat(runtimeData.getWorkflowNodeKey()).isEqualTo("node-key-1");
        assertThat(runtimeData.getWorkflowNodeExecutionId()).isEqualTo("nexec-1");
        assertThat(runtimeData.getExecutionData()).isNotNull();
        assertThat(runtimeData.getExecutionData().workflowExecution()).isEqualTo(workflowExecution);
        assertThat(runtimeData.getExecutionData().nodeExecution()).isEqualTo(nodeExecution);
    }

    @Test
    void shouldHandleNullWorkflow() {
        // setup
        var workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowExecutionId("exec-2");
        workflowExecution.setWorkflowId("wf-2");
        workflowExecution.setWorkflow(null);

        var nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNodeId("node-2");
        nodeExecution.setWorkflowNodeExecutionId("nexec-2");
        nodeExecution.setWorkflowNode(WorkflowNode.builder().workflowNodeKey("nk-2").build());

        // expectation
        var runtimeData = ContextRuntimeData.from(workflowExecution, nodeExecution);

        // validation
        assertThat(runtimeData.getWorkflowDomain()).isNull();
        assertThat(runtimeData.getWorkflowId()).isEqualTo("wf-2");
        assertThat(runtimeData.getWorkflowNodeKey()).isEqualTo("nk-2");
    }

    @Test
    void shouldHandleNullWorkflowNode() {
        // setup
        var workflow = new Workflow();
        workflow.setDomain("domain-3");

        var workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowExecutionId("exec-3");
        workflowExecution.setWorkflowId("wf-3");
        workflowExecution.setWorkflow(workflow);

        var nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNodeId("node-3");
        nodeExecution.setWorkflowNodeExecutionId("nexec-3");
        nodeExecution.setWorkflowNode(null);

        // expectation
        var runtimeData = ContextRuntimeData.from(workflowExecution, nodeExecution);

        // validation
        assertThat(runtimeData.getWorkflowNodeKey()).isNull();
        assertThat(runtimeData.getWorkflowDomain()).isEqualTo("domain-3");
        assertThat(runtimeData.getWorkflowNodeId()).isEqualTo("node-3");
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var runtimeData = new ContextRuntimeData();
        var workflowExecution = new WorkflowExecution();
        var nodeExecution = new WorkflowNodeExecution();
        var executionData = new ContextRuntimeData.ExecutionData(workflowExecution, nodeExecution);

        // expectation
        runtimeData.setWorkflowId("wf-set");
        runtimeData.setWorkflowDomain("domain-set");
        runtimeData.setWorkflowExecutionId("exec-set");
        runtimeData.setWorkflowNodeId("node-set");
        runtimeData.setWorkflowNodeKey("key-set");
        runtimeData.setWorkflowNodeExecutionId("nexec-set");
        runtimeData.setExecutionData(executionData);

        // validation
        assertThat(runtimeData.getWorkflowId()).isEqualTo("wf-set");
        assertThat(runtimeData.getWorkflowDomain()).isEqualTo("domain-set");
        assertThat(runtimeData.getWorkflowExecutionId()).isEqualTo("exec-set");
        assertThat(runtimeData.getWorkflowNodeId()).isEqualTo("node-set");
        assertThat(runtimeData.getWorkflowNodeKey()).isEqualTo("key-set");
        assertThat(runtimeData.getWorkflowNodeExecutionId()).isEqualTo("nexec-set");
        assertThat(runtimeData.getExecutionData()).isEqualTo(executionData);
        assertThat(runtimeData.getExecutionData().workflowExecution()).isEqualTo(workflowExecution);
        assertThat(runtimeData.getExecutionData().nodeExecution()).isEqualTo(nodeExecution);
    }
}
