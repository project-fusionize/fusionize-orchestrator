package dev.fusionize.workflow;

import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutionTest {

    @Test
    void shouldCreateFromWorkflow_withIdleStatus() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");

        // expectation
        var execution = WorkflowExecution.of(workflow);

        // validation
        assertThat(execution.getStatus()).isEqualTo(WorkflowExecutionStatus.IDLE);
    }

    @Test
    void shouldCreateFromWorkflow_withGeneratedId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");

        // expectation
        var execution = WorkflowExecution.of(workflow);

        // validation
        assertThat(execution.getWorkflowExecutionId()).startsWith("WEXE");
    }

    @Test
    void shouldCreateFromWorkflow_withTimestamps() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");

        // expectation
        var execution = WorkflowExecution.of(workflow);

        // validation
        assertThat(execution.getCreatedDate()).isNotNull();
        assertThat(execution.getUpdatedDate()).isNotNull();
    }

    @Test
    void shouldFindNodeInMap_byExecutionId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();
        var nodeExecution = WorkflowNodeExecution.of(node, context);

        var map = new HashMap<String, WorkflowNodeExecution>();
        map.put(nodeExecution.getWorkflowNodeExecutionId(), nodeExecution);
        execution.setNodeExecutionMap(map);

        // expectation
        var found = execution.findNodeByWorkflowNodeExecutionId(nodeExecution.getWorkflowNodeExecutionId());

        // validation
        assertThat(found).isNotNull();
        assertThat(found.getWorkflowNodeExecutionId()).isEqualTo(nodeExecution.getWorkflowNodeExecutionId());
    }

    @Test
    void shouldFindNodeInTree_byExecutionId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();
        var nodeExecution = WorkflowNodeExecution.of(node, context);

        execution.setNodes(new ArrayList<>(List.of(nodeExecution)));

        // expectation
        var found = execution.findNodeByWorkflowNodeExecutionId(nodeExecution.getWorkflowNodeExecutionId());

        // validation
        assertThat(found).isNotNull();
        assertThat(found.getWorkflowNodeExecutionId()).isEqualTo(nodeExecution.getWorkflowNodeExecutionId());
    }

    @Test
    void shouldReturnNull_whenNodeNotFoundByExecutionId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        // expectation
        var found = execution.findNodeByWorkflowNodeExecutionId("non-existent-id");

        // validation
        assertThat(found).isNull();
    }

    @Test
    void shouldFindNodesInMap_byWorkflowNodeId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        var node1 = WorkflowNode.builder().workflowNodeId("shared-node").build();
        var node2 = WorkflowNode.builder().workflowNodeId("shared-node").build();
        var context = Context.builder().build();
        var nodeExecution1 = WorkflowNodeExecution.of(node1, context);
        var nodeExecution2 = WorkflowNodeExecution.of(node2, context);

        var map = new HashMap<String, WorkflowNodeExecution>();
        map.put(nodeExecution1.getWorkflowNodeExecutionId(), nodeExecution1);
        map.put(nodeExecution2.getWorkflowNodeExecutionId(), nodeExecution2);
        execution.setNodeExecutionMap(map);

        // expectation
        var found = execution.findNodesByWorkflowNodeId("shared-node");

        // validation
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(n -> n.getWorkflowNodeId().equals("shared-node"));
    }

    @Test
    void shouldFindNodesInTree_byWorkflowNodeId() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("target-node").build();
        var context = Context.builder().build();

        var parentExecution = WorkflowNodeExecution.of(parentNode, context);
        var childExecution = WorkflowNodeExecution.of(childNode, context);
        parentExecution.setChildren(List.of(childExecution));

        execution.setNodes(new ArrayList<>(List.of(parentExecution)));
        execution.setNodeExecutionMap(new HashMap<>());

        // expectation
        var found = execution.findNodesByWorkflowNodeId("target-node");

        // validation
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getWorkflowNodeId()).isEqualTo("target-node");
    }

    @Test
    void shouldFlattenAndInflate() {
        // setup
        var workflow = new Workflow();
        workflow.setWorkflowId("wf-1");
        var execution = WorkflowExecution.of(workflow);

        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("child").build();
        var context = Context.builder().build();

        var parentExecution = WorkflowNodeExecution.of(parentNode, context);
        var childExecution = WorkflowNodeExecution.of(childNode, context);
        parentExecution.setChildren(new ArrayList<>(List.of(childExecution)));
        parentExecution.setChildrenIds(new ArrayList<>(List.of(childExecution.getWorkflowNodeExecutionId())));

        execution.setNodes(new ArrayList<>(List.of(parentExecution)));

        // expectation
        execution.flatten();

        // validation
        assertThat(execution.getNodeExecutionMap()).isNotEmpty();
        assertThat(execution.getRootNodeExecutionIds()).isNotEmpty();
        assertThat(execution.getNodeExecutionMap()).containsKey(parentExecution.getWorkflowNodeExecutionId());
        assertThat(execution.getNodeExecutionMap()).containsKey(childExecution.getWorkflowNodeExecutionId());

        // expectation
        execution.inflate();

        // validation
        assertThat(execution.getNodes()).isNotEmpty();
        assertThat(execution.getNodes().getFirst().getWorkflowNodeExecutionId())
                .isEqualTo(parentExecution.getWorkflowNodeExecutionId());
    }
}
