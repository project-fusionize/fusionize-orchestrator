package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNodeExecutionState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    @Test
    void builder_ShouldCreateContextWithValues() {
        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("d1");

        WorkflowGraphNode node = new WorkflowGraphNode();
        node.setNode("n1");

        Context context = Context.builder()
                .add("key1", "value1")
                .add("key2", 123)
                .decisions(decision)
                .graphNodes(node)
                .build();

        assertNotNull(context);
        assertEquals("value1", context.getData().get("key1"));
        assertEquals(123, context.getData().get("key2"));
        assertEquals(1, context.getDecisions().size());
        assertEquals("d1", context.getDecisions().get(0).getDecisionNode());
        assertEquals(1, context.getGraphNodes().size());
        assertEquals("n1", context.getGraphNodes().get(0).getNode());
    }

    @Test
    void builder_ShouldAddAll() {
        Map<String, Object> map = Map.of("k1", "v1", "k2", "v2");
        Context context = Context.builder()
                .addAll(map)
                .build();

        assertEquals(2, context.getData().size());
        assertEquals("v1", context.getData().get("k1"));
        assertEquals("v2", context.getData().get("k2"));
    }

    @Test
    void renew_ShouldDeepCopyCollections() {
        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("d1");

        WorkflowGraphNode node = new WorkflowGraphNode();
        node.setNode("n1");
        node.setState(WorkflowNodeExecutionState.IDLE);

        Context original = Context.builder()
                .add("key", "value")
                .decisions(decision)
                .graphNodes(node)
                .build();

        Context copy = original.renew();

        assertNotSame(original, copy);
        assertNotSame(original.getData(), copy.getData());
        assertNotSame(original.getDecisions(), copy.getDecisions());
        assertNotSame(original.getGraphNodes(), copy.getGraphNodes());

        assertEquals(original.getData(), copy.getData());

        // Verify decision deep copy (assuming WorkflowDecision.renew works)
        assertEquals(1, copy.getDecisions().size());
        assertEquals("d1", copy.getDecisions().get(0).getDecisionNode());

        // Verify graph node deep copy (assuming WorkflowGraphNode.renew works)
        assertEquals(1, copy.getGraphNodes().size());
        assertEquals("n1", copy.getGraphNodes().get(0).getNode());
    }

    @Test
    void var_ShouldReturnTypedOptional() {
        Context context = Context.builder()
                .add("string", "hello")
                .add("int", 123)
                .add("double", 12.34)
                .add("list", List.of("a", "b"))
                .build();

        // String
        Optional<String> s = context.varString("string");
        assertTrue(s.isPresent());
        assertEquals("hello", s.get());

        // Integer
        Optional<Integer> i = context.varInt("int");
        assertTrue(i.isPresent());
        assertEquals(123, i.get());

        // Double
        Optional<Double> d = context.varDouble("double");
        assertTrue(d.isPresent());
        assertEquals(12.34, d.get());

        // List
        Optional<List> l = context.varList("list");
        assertTrue(l.isPresent());
        assertEquals(2, l.get().size());

        // Missing key
        assertTrue(context.varString("missing").isEmpty());

        // Wrong type
        assertTrue(context.varInt("string").isEmpty());
    }

    @Test
    void contains_ShouldReturnTrueIfKeyExists() {
        Context context = Context.builder()
                .add("key", "value")
                .build();

        assertTrue(context.contains("key"));
        assertFalse(context.contains("missing"));
    }

    @Test
    void set_ShouldUpdateValue() {
        Context context = new Context();
        context.set("key", "value1");
        assertEquals("value1", context.getData().get("key"));

        context.set("key", "value2");
        assertEquals("value2", context.getData().get("key"));
    }
}
