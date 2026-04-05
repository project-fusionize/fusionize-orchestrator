package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeExecutionGraphAdapterTest {

    private final WorkflowNodeExecutionGraphAdapter adapter = new WorkflowNodeExecutionGraphAdapter();

    @Test
    void shouldReturnId() {
        // setup
        var node = new WorkflowNodeExecution();
        node.setWorkflowNodeExecutionId("exec-1");

        // expectation
        String result = adapter.getId(node);

        // validation
        assertThat(result).isEqualTo("exec-1");
    }

    @Test
    void shouldReturnChildren() {
        // setup
        var node = new WorkflowNodeExecution();
        var child1 = new WorkflowNodeExecution();
        child1.setWorkflowNodeExecutionId("child-1");
        var child2 = new WorkflowNodeExecution();
        child2.setWorkflowNodeExecutionId("child-2");
        node.setChildren(List.of(child1, child2));

        // expectation
        Collection<WorkflowNodeExecution> result = adapter.getChildren(node);

        // validation
        assertThat(result).containsExactly(child1, child2);
    }

    @Test
    void shouldSetChildren() {
        // setup
        var node = new WorkflowNodeExecution();
        var child1 = new WorkflowNodeExecution();
        child1.setWorkflowNodeExecutionId("child-1");
        Collection<WorkflowNodeExecution> children = List.of(child1);

        // expectation
        adapter.setChildren(node, children);

        // validation
        assertThat(node.getChildren()).containsExactly(child1);
    }

    @Test
    void shouldReturnChildrenIds() {
        // setup
        var node = new WorkflowNodeExecution();
        node.setChildrenIds(List.of("id-1", "id-2"));

        // expectation
        Collection<String> result = adapter.getChildrenIds(node);

        // validation
        assertThat(result).containsExactly("id-1", "id-2");
    }

    @Test
    void shouldSetChildrenIds() {
        // setup
        var node = new WorkflowNodeExecution();
        Collection<String> ids = List.of("id-a", "id-b");

        // expectation
        adapter.setChildrenIds(node, ids);

        // validation
        assertThat(node.getChildrenIds()).containsExactly("id-a", "id-b");
    }
}
