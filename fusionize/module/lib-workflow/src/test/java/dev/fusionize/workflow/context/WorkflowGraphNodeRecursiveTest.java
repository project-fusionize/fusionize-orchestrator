package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowGraphNodeRecursiveTest {

    @Test
    void shouldInitializeWithNodeAndState() {
        // setup
        var node = new WorkflowGraphNodeRecursive("node-1", WorkflowNodeExecutionState.WORKING);

        // expectation
        String nodeName = node.getNode();
        WorkflowNodeExecutionState state = node.getState();

        // validation
        assertThat(nodeName).isEqualTo("node-1");
        assertThat(state).isEqualTo(WorkflowNodeExecutionState.WORKING);
    }

    @Test
    void shouldDefaultToEmptyParentsAndChildren() {
        // setup
        var node = new WorkflowGraphNodeRecursive("node-1", WorkflowNodeExecutionState.IDLE);

        // expectation
        var parents = node.getParents();
        var children = node.getChildren();

        // validation
        assertThat(parents).isEmpty();
        assertThat(children).isEmpty();
    }

    @Test
    void shouldSetAndGetParents() {
        // setup
        var node = new WorkflowGraphNodeRecursive("node-1", WorkflowNodeExecutionState.IDLE);
        var parent1 = new WorkflowGraphNodeRecursive("parent-1", WorkflowNodeExecutionState.DONE);
        var parent2 = new WorkflowGraphNodeRecursive("parent-2", WorkflowNodeExecutionState.DONE);

        // expectation
        node.setParents(List.of(parent1, parent2));

        // validation
        assertThat(node.getParents()).containsExactly(parent1, parent2);
    }

    @Test
    void shouldSetAndGetChildren() {
        // setup
        var node = new WorkflowGraphNodeRecursive("node-1", WorkflowNodeExecutionState.IDLE);
        var child1 = new WorkflowGraphNodeRecursive("child-1", WorkflowNodeExecutionState.IDLE);
        var child2 = new WorkflowGraphNodeRecursive("child-2", WorkflowNodeExecutionState.WORKING);

        // expectation
        node.setChildren(List.of(child1, child2));

        // validation
        assertThat(node.getChildren()).containsExactly(child1, child2);
    }

    @Test
    void shouldReturnMeaningfulToString() {
        // setup
        var node = new WorkflowGraphNodeRecursive("node-1", WorkflowNodeExecutionState.WORKING);
        var parent = new WorkflowGraphNodeRecursive("parent-1", WorkflowNodeExecutionState.DONE);
        var child = new WorkflowGraphNodeRecursive("child-1", WorkflowNodeExecutionState.IDLE);
        node.setParents(List.of(parent));
        node.setChildren(List.of(child));

        // expectation
        String result = node.toString();

        // validation
        assertThat(result).contains("node-1");
        assertThat(result).contains("WORKING");
        assertThat(result).contains("parent-1");
        assertThat(result).contains("child-1");
    }
}
