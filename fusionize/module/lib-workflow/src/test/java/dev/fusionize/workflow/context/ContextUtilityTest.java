package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContextUtilityTest {

    @Test
    void extractCurrentNodes_ShouldReturnLeafNodes() {
        // Create graph: root -> child1 -> leaf1
        // -> child2 (leaf)

        WorkflowGraphNode root = new WorkflowGraphNode();
        root.setNode("root");
        root.setState(WorkflowNodeExecutionState.DONE);

        WorkflowGraphNode child1 = new WorkflowGraphNode();
        child1.setNode("child1");
        child1.setState(WorkflowNodeExecutionState.DONE);
        child1.getParents().add("root");

        WorkflowGraphNode leaf1 = new WorkflowGraphNode();
        leaf1.setNode("leaf1");
        leaf1.setState(WorkflowNodeExecutionState.IDLE);
        leaf1.getParents().add("child1");

        WorkflowGraphNode child2 = new WorkflowGraphNode();
        child2.setNode("child2");
        child2.setState(WorkflowNodeExecutionState.IDLE);
        child2.getParents().add("root");

        Context context = Context.builder()
                .graphNodes(root, child1, leaf1, child2)
                .build();

        List<WorkflowGraphNodeRecursive> leaves = ContextUtility.extractCurrentNodes(context);

        assertNotNull(leaves);
        assertEquals(2, leaves.size());

        // Verify leaf1
        WorkflowGraphNodeRecursive rLeaf1 = leaves.stream()
                .filter(n -> "leaf1".equals(n.getNode()))
                .findFirst()
                .orElse(null);
        assertNotNull(rLeaf1);
        assertEquals(WorkflowNodeExecutionState.IDLE, rLeaf1.getState());
        assertEquals(1, rLeaf1.getParents().size());
        assertEquals("child1", rLeaf1.getParents().get(0).getNode());

        // Verify child2
        WorkflowGraphNodeRecursive rChild2 = leaves.stream()
                .filter(n -> "child2".equals(n.getNode()))
                .findFirst()
                .orElse(null);
        assertNotNull(rChild2);
        assertEquals(WorkflowNodeExecutionState.IDLE, rChild2.getState());
        assertEquals(1, rChild2.getParents().size());
        assertEquals("root", rChild2.getParents().get(0).getNode());
    }

    @Test
    void extractCurrentNodes_WithEmptyContext_ShouldReturnEmptyList() {
        assertTrue(ContextUtility.extractCurrentNodes(null).isEmpty());
        assertTrue(ContextUtility.extractCurrentNodes(new Context()).isEmpty());
    }

    @Test
    void getLatestDecisionForNode_ShouldReturnLatestMatch() {
        WorkflowDecision d1 = new WorkflowDecision();
        d1.setDecisionNode("node1");
        // Add some dummy option to distinguish if needed, but order matters

        WorkflowDecision d2 = new WorkflowDecision();
        d2.setDecisionNode("node2");

        WorkflowDecision d3 = new WorkflowDecision();
        d3.setDecisionNode("node1"); // Newer decision for node1

        Context context = Context.builder()
                .decisions(d1, d2, d3)
                .build();

        WorkflowDecision latest = ContextUtility.getLatestDecisionForNode(context, "node1");

        assertNotNull(latest);
        assertEquals("node1", latest.getDecisionNode());
        assertSame(d3, latest); // Should be the last one added
    }

    @Test
    void getLatestDecisionForNode_WithNoMatch_ShouldReturnNull() {
        WorkflowDecision d1 = new WorkflowDecision();
        d1.setDecisionNode("node1");

        Context context = Context.builder()
                .decisions(d1)
                .build();

        assertNull(ContextUtility.getLatestDecisionForNode(context, "node2"));
    }

    @Test
    void getLatestDecisionForNode_WithEmptyContext_ShouldReturnNull() {
        assertNull(ContextUtility.getLatestDecisionForNode(null, "node1"));
        assertNull(ContextUtility.getLatestDecisionForNode(new Context(), "node1"));
    }
}
