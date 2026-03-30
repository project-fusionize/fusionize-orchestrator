package dev.fusionize.workflow;

import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeExecutionTest {

    @Test
    void shouldCreateFromNode_withIdleState() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();

        // expectation
        var execution = WorkflowNodeExecution.of(node, context);

        // validation
        assertThat(execution.getState()).isEqualTo(WorkflowNodeExecutionState.IDLE);
    }

    @Test
    void shouldCreateFromNode_withGeneratedId() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();

        // expectation
        var execution = WorkflowNodeExecution.of(node, context);

        // validation
        assertThat(execution.getWorkflowNodeExecutionId()).startsWith("NEXE");
    }

    @Test
    void shouldCreateFromNode_withTimestamps() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();

        // expectation
        var execution = WorkflowNodeExecution.of(node, context);

        // validation
        assertThat(execution.getCreatedDate()).isNotNull();
        assertThat(execution.getUpdatedDate()).isNotNull();
    }

    @Test
    void shouldFindNodeByExecutionId_inDirectChildren() {
        // setup
        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("child").build();
        var context = Context.builder().build();

        var parent = WorkflowNodeExecution.of(parentNode, context);
        var child = WorkflowNodeExecution.of(childNode, context);
        parent.setChildren(List.of(child));

        // expectation
        var found = parent.findNodeByWorkflowNodeExecutionId(child.getWorkflowNodeExecutionId());

        // validation
        assertThat(found).isNotNull();
        assertThat(found.getWorkflowNodeExecutionId()).isEqualTo(child.getWorkflowNodeExecutionId());
    }

    @Test
    void shouldFindNodeByExecutionId_inNestedChildren() {
        // setup
        var grandparentNode = WorkflowNode.builder().workflowNodeId("grandparent").build();
        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("child").build();
        var context = Context.builder().build();

        var grandparent = WorkflowNodeExecution.of(grandparentNode, context);
        var parent = WorkflowNodeExecution.of(parentNode, context);
        var child = WorkflowNodeExecution.of(childNode, context);

        parent.setChildren(List.of(child));
        grandparent.setChildren(List.of(parent));

        // expectation
        var found = grandparent.findNodeByWorkflowNodeExecutionId(child.getWorkflowNodeExecutionId());

        // validation
        assertThat(found).isNotNull();
        assertThat(found.getWorkflowNodeExecutionId()).isEqualTo(child.getWorkflowNodeExecutionId());
    }

    @Test
    void shouldReturnNull_whenNodeNotFound() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();
        var execution = WorkflowNodeExecution.of(node, context);

        // expectation
        var found = execution.findNodeByWorkflowNodeExecutionId("non-existent-id");

        // validation
        assertThat(found).isNull();
    }

    @Test
    void shouldFindNodesByWorkflowNodeId_inDirectChildren() {
        // setup
        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode1 = WorkflowNode.builder().workflowNodeId("shared-id").build();
        var childNode2 = WorkflowNode.builder().workflowNodeId("shared-id").build();
        var context = Context.builder().build();

        var parent = WorkflowNodeExecution.of(parentNode, context);
        var child1 = WorkflowNodeExecution.of(childNode1, context);
        var child2 = WorkflowNodeExecution.of(childNode2, context);
        parent.setChildren(List.of(child1, child2));

        // expectation
        var found = parent.findNodesByWorkflowNodeId("shared-id");

        // validation
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(n -> n.getWorkflowNodeId().equals("shared-id"));
    }

    @Test
    void shouldFindNodesByWorkflowNodeId_inNestedChildren() {
        // setup
        var grandparentNode = WorkflowNode.builder().workflowNodeId("grandparent").build();
        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("target-id").build();
        var context = Context.builder().build();

        var grandparent = WorkflowNodeExecution.of(grandparentNode, context);
        var parent = WorkflowNodeExecution.of(parentNode, context);
        var child = WorkflowNodeExecution.of(childNode, context);

        parent.setChildren(List.of(child));
        grandparent.setChildren(List.of(parent));

        // expectation
        var found = grandparent.findNodesByWorkflowNodeId("target-id");

        // validation
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getWorkflowNodeId()).isEqualTo("target-id");
    }

    @Test
    void shouldReturnEmptyList_whenNoMatchingNodes() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();
        var execution = WorkflowNodeExecution.of(node, context);

        // expectation
        var found = execution.findNodesByWorkflowNodeId("non-existent-id");

        // validation
        assertThat(found).isEmpty();
    }

    @Test
    void shouldRenewWithNewIdAndIdleState() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var context = Context.builder().build();
        var original = WorkflowNodeExecution.of(node, context);
        original.setState(WorkflowNodeExecutionState.DONE);

        // expectation
        var renewed = original.renew();

        // validation
        assertThat(renewed.getWorkflowNodeExecutionId()).isNotEqualTo(original.getWorkflowNodeExecutionId());
        assertThat(renewed.getWorkflowNodeExecutionId()).startsWith("NEXE");
        assertThat(renewed.getState()).isEqualTo(WorkflowNodeExecutionState.IDLE);
        assertThat(renewed.getWorkflowNodeId()).isEqualTo("node-1");
    }

    @Test
    void shouldRenewChildren_recursively() {
        // setup
        var parentNode = WorkflowNode.builder().workflowNodeId("parent").build();
        var childNode = WorkflowNode.builder().workflowNodeId("child").build();
        var context = Context.builder().build();

        var parent = WorkflowNodeExecution.of(parentNode, context);
        var child = WorkflowNodeExecution.of(childNode, context);
        parent.setChildren(List.of(child));

        // expectation
        var renewed = parent.renew();

        // validation
        assertThat(renewed.getChildren()).hasSize(1);
        assertThat(renewed.getChildren().getFirst().getWorkflowNodeExecutionId())
                .isNotEqualTo(child.getWorkflowNodeExecutionId());
        assertThat(renewed.getChildren().getFirst().getWorkflowNodeId()).isEqualTo("child");
        assertThat(renewed.getChildren().getFirst().getState()).isEqualTo(WorkflowNodeExecutionState.IDLE);
    }
}
