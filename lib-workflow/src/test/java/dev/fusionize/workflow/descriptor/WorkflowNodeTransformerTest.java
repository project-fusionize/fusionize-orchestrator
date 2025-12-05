package dev.fusionize.workflow.descriptor;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowNodeTransformerTest {

    private WorkflowNodeTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new WorkflowNodeTransformer();
    }

    @Test
    void toWorkflowNode_WithValidDescription_ShouldTransformCorrectly() {
        // Given
        WorkflowNodeDescription description = new WorkflowNodeDescription();
        description.setType(WorkflowNodeType.TASK);
        description.setActor("system");
        description.setComponent("test.sendEmail");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("address", "test@example.com");
        configMap.put("retryCount", 3);
        description.setConfig(configMap);
        description.setNext(new ArrayList<>());

        // When
        WorkflowNode node = transformer.toWorkflowNode(description, "node1");

        // Then
        assertNotNull(node);
        assertEquals(WorkflowNodeType.TASK, node.getType());
        assertEquals("system:test.sendEmail", node.getComponent());
        assertEquals("node1", node.getWorkflowNodeKey());
        assertNotNull(node.getComponentConfig());
        assertEquals("test@example.com", node.getComponentConfig().getConfig().get("address"));
        assertEquals(3, node.getComponentConfig().getConfig().get("retryCount"));
    }

    @Test
    void toWorkflowNode_WithNullDescription_ShouldReturnNull() {
        // When
        WorkflowNode node = transformer.toWorkflowNode(null, "node1");

        // Then
        assertNull(node);
    }

    @Test
    void toWorkflowNode_WithEmptyComponentConfig_ShouldCreateNodeWithoutConfig() {
        // Given
        WorkflowNodeDescription description = new WorkflowNodeDescription();
        description.setType(WorkflowNodeType.START);
        description.setActor("system");
        description.setComponent("test.receiveEmail");
        description.setConfig(new HashMap<>());
        description.setNext(new ArrayList<>());

        // When
        WorkflowNode node = transformer.toWorkflowNode(description, "startNode");

        // Then
        assertNotNull(node);
        assertEquals(WorkflowNodeType.START, node.getType());
        assertEquals("system:test.receiveEmail", node.getComponent());
        assertNull(node.getComponentConfig());
    }

    @Test
    void toWorkflowNode_WithNullComponentConfig_ShouldCreateNodeWithoutConfig() {
        // Given
        WorkflowNodeDescription description = new WorkflowNodeDescription();
        description.setType(WorkflowNodeType.END);
        description.setActor("system");
        description.setComponent("test.complete");
        description.setConfig(null);
        description.setNext(new ArrayList<>());

        // When
        WorkflowNode node = transformer.toWorkflowNode(description, "endNode");

        // Then
        assertNotNull(node);
        assertEquals(WorkflowNodeType.END, node.getType());
        assertNull(node.getComponentConfig());
    }

    @Test
    void toWorkflowNodeDescription_WithValidNode_ShouldTransformCorrectly() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.DECISION)
                .component("decision:test.emailDecision")
                .workflowNodeKey("decisionNode")
                .componentConfig(ComponentConfig.builder()
                        .put("routeMap", Map.of("route1", "node1", "route2", "node2"))
                        .put("defaultRoute", "node1")
                        .build())
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(node);

        // Then
        assertNotNull(description);
        assertEquals(WorkflowNodeType.DECISION, description.getType());
        assertEquals("decision", description.getActor());
        assertEquals("test.emailDecision", description.getComponent());
        assertNotNull(description.getConfig());
        assertEquals(2, description.getConfig().size());
        assertTrue(description.getConfig().containsKey("routeMap"));
        assertTrue(description.getConfig().containsKey("defaultRoute"));
        assertNotNull(description.getNext());
        assertTrue(description.getNext().isEmpty());
    }

    @Test
    void toWorkflowNodeDescription_WithNullNode_ShouldReturnNull() {
        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(null);

        // Then
        assertNull(description);
    }

    @Test
    void toWorkflowNodeDescription_WithNodeAndChildren_ShouldIncludeNextKeys() {
        // Given
        WorkflowNode child1 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task1")
                .workflowNodeKey("child1")
                .build();

        WorkflowNode child2 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task2")
                .workflowNodeKey("child2")
                .build();

        WorkflowNode parent = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .component("start:test.start")
                .workflowNodeKey("parent")
                .addChild(child1)
                .addChild(child2)
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(parent);

        // Then
        assertNotNull(description);
        assertEquals(WorkflowNodeType.START, description.getType());
        assertNotNull(description.getNext());
        assertEquals(2, description.getNext().size());
        assertTrue(description.getNext().contains("child1"));
        assertTrue(description.getNext().contains("child2"));
    }

    @Test
    void toWorkflowNodeDescription_WithEmptyComponentConfig_ShouldReturnEmptyMap() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("taskNode")
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(node);

        // Then
        assertNotNull(description);
        assertNotNull(description.getConfig());
        assertTrue(description.getConfig().isEmpty());
    }

    @Test
    void toWorkflowNodeDescription_WithNullComponentConfig_ShouldReturnEmptyMap() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .component("end:test.end")
                .workflowNodeKey("endNode")
                .componentConfig(null)
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(node);

        // Then
        assertNotNull(description);
        assertNotNull(description.getConfig());
        assertTrue(description.getConfig().isEmpty());
    }

    @Test
    void toWorkflowNodeDescription_WithNullChildren_ShouldReturnEmptyNextList() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("taskNode")
                .children(null)
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(node);

        // Then
        assertNotNull(description);
        assertNotNull(description.getNext());
        assertTrue(description.getNext().isEmpty());
    }

    @Test
    void toWorkflowNodeDescription_WithChildrenMissingKeys_ShouldFilterOutNullKeys() {
        // Given
        WorkflowNode child1 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task1")
                .workflowNodeKey("child1")
                .build();

        WorkflowNode child2 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task2")
                .workflowNodeKey(null) // Missing key
                .build();

        WorkflowNode child3 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task3")
                .workflowNodeKey("") // Empty key
                .build();

        WorkflowNode parent = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .component("start:test.start")
                .workflowNodeKey("parent")
                .addChild(child1)
                .addChild(child2)
                .addChild(child3)
                .build();

        // When
        WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(parent);

        // Then
        assertNotNull(description);
        assertEquals(1, description.getNext().size());
        assertTrue(description.getNext().contains("child1"));
        assertFalse(description.getNext().contains(null));
        assertFalse(description.getNext().contains(""));
    }

    @Test
    void roundTripTransformation_ShouldPreserveData() {
        // Given
        WorkflowNodeDescription original = new WorkflowNodeDescription();
        original.setType(WorkflowNodeType.TASK);
        original.setActor("ai");
        original.setComponent("test.sendEmail");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("address", "test@example.com");
        configMap.put("subject", "Test Email");
        configMap.put("retryCount", 3);
        original.setConfig(configMap);
        original.setNext(List.of("nextNode1", "nextNode2"));

        // When: Transform to node and back
        WorkflowNode node = transformer.toWorkflowNode(original, "testNode");
        WorkflowNodeDescription result = transformer.toWorkflowNodeDescription(node);

        // Then
        assertNotNull(result);
        assertEquals(original.getType(), result.getType());
        assertEquals(original.getActor(), result.getActor());
        assertEquals(original.getComponent(), result.getComponent());
        assertEquals(original.getConfig().size(), result.getConfig().size());
        assertEquals(original.getConfig().get("address"), result.getConfig().get("address"));
        assertEquals(original.getConfig().get("subject"), result.getConfig().get("subject"));
        assertEquals(original.getConfig().get("retryCount"), result.getConfig().get("retryCount"));
        // Note: next field is not preserved in this round trip because children are set
        // separately
    }
}
