package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowGraphNodeTest {

    @Test
    void shouldDefaultStateToIdle() {
        // setup
        var graphNode = new WorkflowGraphNode();

        // expectation
        WorkflowNodeExecutionState state = graphNode.getState();

        // validation
        assertThat(state).isEqualTo(WorkflowNodeExecutionState.IDLE);
    }

    @Test
    void shouldSetAndGetNode() {
        // setup
        var graphNode = new WorkflowGraphNode();

        // expectation
        graphNode.setNode("test-node");

        // validation
        assertThat(graphNode.getNode()).isEqualTo("test-node");
    }

    @Test
    void shouldSetAndGetState() {
        // setup
        var graphNode = new WorkflowGraphNode();

        // expectation
        graphNode.setState(WorkflowNodeExecutionState.WORKING);

        // validation
        assertThat(graphNode.getState()).isEqualTo(WorkflowNodeExecutionState.WORKING);
    }

    @Test
    void shouldSetAndGetParents() {
        // setup
        var graphNode = new WorkflowGraphNode();
        var parents = List.of("parent-1", "parent-2");

        // expectation
        graphNode.setParents(parents);

        // validation
        assertThat(graphNode.getParents()).containsExactly("parent-1", "parent-2");
    }

    @Test
    void shouldRenewWithDeepCopy() {
        // setup
        var original = new WorkflowGraphNode();
        original.setNode("node-1");
        original.setState(WorkflowNodeExecutionState.DONE);
        original.setParents(new ArrayList<>(List.of("parent-1")));

        // expectation
        var copy = original.renew();
        original.setNode("modified-node");
        original.setState(WorkflowNodeExecutionState.FAILED);
        original.getParents().add("parent-2");

        // validation
        assertThat(copy.getNode()).isEqualTo("node-1");
        assertThat(copy.getState()).isEqualTo(WorkflowNodeExecutionState.DONE);
        assertThat(copy.getParents()).containsExactly("parent-1");
    }

    @Test
    void shouldBeEqualWhenSameFields() {
        // setup
        var node1 = new WorkflowGraphNode();
        node1.setNode("node-1");
        node1.setState(WorkflowNodeExecutionState.IDLE);
        node1.setParents(List.of("parent-1"));

        var node2 = new WorkflowGraphNode();
        node2.setNode("node-1");
        node2.setState(WorkflowNodeExecutionState.IDLE);
        node2.setParents(List.of("parent-1"));

        // expectation
        boolean result = node1.equals(node2);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenDifferentNode() {
        // setup
        var node1 = new WorkflowGraphNode();
        node1.setNode("node-1");

        var node2 = new WorkflowGraphNode();
        node2.setNode("node-2");

        // expectation
        boolean result = node1.equals(node2);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenDifferentState() {
        // setup
        var node1 = new WorkflowGraphNode();
        node1.setNode("node-1");
        node1.setState(WorkflowNodeExecutionState.IDLE);

        var node2 = new WorkflowGraphNode();
        node2.setNode("node-1");
        node2.setState(WorkflowNodeExecutionState.DONE);

        // expectation
        boolean result = node1.equals(node2);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldHaveConsistentHashCode() {
        // setup
        var node1 = new WorkflowGraphNode();
        node1.setNode("node-1");
        node1.setState(WorkflowNodeExecutionState.IDLE);
        node1.setParents(List.of("parent-1"));

        var node2 = new WorkflowGraphNode();
        node2.setNode("node-1");
        node2.setState(WorkflowNodeExecutionState.IDLE);
        node2.setParents(List.of("parent-1"));

        // expectation
        int hash1 = node1.hashCode();
        int hash2 = node2.hashCode();

        // validation
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldReturnMeaningfulToString() {
        // setup
        var graphNode = new WorkflowGraphNode();
        graphNode.setNode("node-1");
        graphNode.setState(WorkflowNodeExecutionState.IDLE);
        graphNode.setParents(List.of("parent-1"));

        // expectation
        String result = graphNode.toString();

        // validation
        assertThat(result).contains("node-1");
        assertThat(result).contains("IDLE");
        assertThat(result).contains("parent-1");
    }
}
