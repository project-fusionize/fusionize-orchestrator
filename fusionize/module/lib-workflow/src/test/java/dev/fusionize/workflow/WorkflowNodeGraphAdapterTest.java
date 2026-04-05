package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeGraphAdapterTest {

    private final WorkflowNodeGraphAdapter adapter = new WorkflowNodeGraphAdapter();

    @Test
    void shouldReturnId() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();

        // expectation
        String result = adapter.getId(node);

        // validation
        assertThat(result).isEqualTo("node-1");
    }

    @Test
    void shouldReturnChildren() {
        // setup
        var child1 = WorkflowNode.builder().workflowNodeId("child-1").build();
        var child2 = WorkflowNode.builder().workflowNodeId("child-2").build();
        var node = WorkflowNode.builder()
                .workflowNodeId("node-1")
                .children(List.of(child1, child2))
                .build();

        // expectation
        Collection<WorkflowNode> result = adapter.getChildren(node);

        // validation
        assertThat(result).containsExactly(child1, child2);
    }

    @Test
    void shouldSetChildren() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var child1 = WorkflowNode.builder().workflowNodeId("child-1").build();
        Collection<WorkflowNode> children = List.of(child1);

        // expectation
        adapter.setChildren(node, children);

        // validation
        assertThat(node.getChildren()).containsExactly(child1);
    }

    @Test
    void shouldReturnChildrenIds() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        node.setChildrenIds(List.of("id-1", "id-2"));

        // expectation
        Collection<String> result = adapter.getChildrenIds(node);

        // validation
        assertThat(result).containsExactly("id-1", "id-2");
    }

    @Test
    void shouldSetChildrenIds() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        Collection<String> ids = List.of("id-a", "id-b");

        // expectation
        adapter.setChildrenIds(node, ids);

        // validation
        assertThat(node.getChildrenIds()).containsExactly("id-a", "id-b");
    }

    @Test
    void shouldReturnSecondaryChildren() {
        // setup
        var comp1 = WorkflowNode.builder().workflowNodeId("comp-1").build();
        var comp2 = WorkflowNode.builder().workflowNodeId("comp-2").build();
        var node = WorkflowNode.builder()
                .workflowNodeId("node-1")
                .compensateNodes(List.of(comp1, comp2))
                .build();

        // expectation
        Collection<WorkflowNode> result = adapter.getSecondaryChildren(node);

        // validation
        assertThat(result).containsExactly(comp1, comp2);
    }

    @Test
    void shouldSetSecondaryChildren() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        var comp1 = WorkflowNode.builder().workflowNodeId("comp-1").build();
        Collection<WorkflowNode> compensateNodes = List.of(comp1);

        // expectation
        adapter.setSecondaryChildren(node, compensateNodes);

        // validation
        assertThat(node.getCompensateNodes()).containsExactly(comp1);
    }

    @Test
    void shouldReturnSecondaryChildrenIds() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        node.setCompensateNodeIds(List.of("comp-id-1", "comp-id-2"));

        // expectation
        Collection<String> result = adapter.getSecondaryChildrenIds(node);

        // validation
        assertThat(result).containsExactly("comp-id-1", "comp-id-2");
    }

    @Test
    void shouldSetSecondaryChildrenIds() {
        // setup
        var node = WorkflowNode.builder().workflowNodeId("node-1").build();
        Collection<String> ids = List.of("comp-id-a", "comp-id-b");

        // expectation
        adapter.setSecondaryChildrenIds(node, ids);

        // validation
        assertThat(node.getCompensateNodeIds()).containsExactly("comp-id-a", "comp-id-b");
    }
}
