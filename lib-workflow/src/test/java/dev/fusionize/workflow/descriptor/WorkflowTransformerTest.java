package dev.fusionize.workflow.descriptor;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTransformerTest {

    private WorkflowTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new WorkflowTransformer();
    }

    @Test
    void toWorkflow_WithValidDescription_ShouldTransformCorrectly() {
        // Given
        WorkflowDescription description = createSimpleWorkflowDescription();

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then
        assertNotNull(workflow);
        assertEquals(description.getName(), workflow.getName());
        assertEquals(description.getDomain(), workflow.getDomain());
        assertEquals(description.getKey(), workflow.getKey());
        assertEquals(description.getDescription(), workflow.getDescription());
        assertEquals(description.getVersion(), workflow.getVersion());
        assertEquals(description.isActive(), workflow.isActive());
    }

    @Test
    void toWorkflow_WithNullDescription_ShouldReturnNull() {
        // When
        Workflow workflow = transformer.toWorkflow(null);

        // Then
        assertNull(workflow);
    }

    @Test
    void toWorkflow_WithEmptyNodes_ShouldCreateWorkflowWithEmptyNodeList() {
        // Given
        WorkflowDescription description = createSimpleWorkflowDescription();
        description.setNodes(new HashMap<>());

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then
        assertNotNull(workflow);
        assertNotNull(workflow.getNodes());
        assertTrue(workflow.getNodes().isEmpty());
    }

    @Test
    void toWorkflow_WithSimpleNodeHierarchy_ShouldBuildCorrectRelationships() {
        // Given: start -> task -> end
        WorkflowDescription description = createSimpleWorkflowDescription();

        WorkflowNodeDescription startNode = createNodeDescription(WorkflowNodeType.START, "start:test.start",
                List.of("task1"));
        WorkflowNodeDescription taskNode = createNodeDescription(WorkflowNodeType.TASK, "task:test.task",
                List.of("end1"));
        WorkflowNodeDescription endNode = createNodeDescription(WorkflowNodeType.END, "end:test.end",
                new ArrayList<>());

        Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
        nodes.put("start1", startNode);
        nodes.put("task1", taskNode);
        nodes.put("end1", endNode);
        description.setNodes(nodes);

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then
        assertNotNull(workflow);
        assertNotNull(workflow.getNodes());
        assertEquals(1, workflow.getNodes().size()); // Only root node (start1)

        WorkflowNode rootNode = workflow.getNodes().get(0);
        assertEquals("start1", rootNode.getWorkflowNodeKey());
        assertEquals(1, rootNode.getChildren().size());

        WorkflowNode taskNodeResult = rootNode.getChildren().get(0);
        assertEquals("task1", taskNodeResult.getWorkflowNodeKey());
        assertEquals(1, taskNodeResult.getChildren().size());

        WorkflowNode endNodeResult = taskNodeResult.getChildren().get(0);
        assertEquals("end1", endNodeResult.getWorkflowNodeKey());
        assertTrue(endNodeResult.getChildren().isEmpty());
    }

    @Test
    void toWorkflow_WithComplexNodeHierarchy_ShouldBuildCorrectRelationships() {
        // Given: start -> decision -> (task1, task2) -> end
        WorkflowDescription description = createSimpleWorkflowDescription();

        WorkflowNodeDescription startNode = createNodeDescription(WorkflowNodeType.START, "start:test.start",
                List.of("decision1"));
        WorkflowNodeDescription decisionNode = createNodeDescription(WorkflowNodeType.DECISION,
                "decision:test.decision", List.of("task1", "task2"));
        WorkflowNodeDescription task1Node = createNodeDescription(WorkflowNodeType.TASK, "task:test.task1",
                List.of("end1"));
        WorkflowNodeDescription task2Node = createNodeDescription(WorkflowNodeType.TASK, "task:test.task2",
                List.of("end1"));
        WorkflowNodeDescription endNode = createNodeDescription(WorkflowNodeType.END, "end:test.end",
                new ArrayList<>());

        Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
        nodes.put("start1", startNode);
        nodes.put("decision1", decisionNode);
        nodes.put("task1", task1Node);
        nodes.put("task2", task2Node);
        nodes.put("end1", endNode);
        description.setNodes(nodes);

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then
        assertNotNull(workflow);
        assertEquals(1, workflow.getNodes().size()); // Only root node

        WorkflowNode root = workflow.getNodes().get(0);
        assertEquals("start1", root.getWorkflowNodeKey());
        assertEquals(1, root.getChildren().size());

        WorkflowNode decision = root.getChildren().get(0);
        assertEquals("decision1", decision.getWorkflowNodeKey());
        assertEquals(2, decision.getChildren().size());

        List<String> taskKeys = decision.getChildren().stream()
                .map(WorkflowNode::getWorkflowNodeKey)
                .toList();
        assertTrue(taskKeys.contains("task1"));
        assertTrue(taskKeys.contains("task2"));

        // Both tasks should point to the same end node
        WorkflowNode task1 = decision.getChildren().stream()
                .filter(n -> "task1".equals(n.getWorkflowNodeKey()))
                .findFirst().orElse(null);
        assertNotNull(task1);
        assertEquals(1, task1.getChildren().size());
        assertEquals("end1", task1.getChildren().get(0).getWorkflowNodeKey());
    }

    @Test
    void toWorkflow_WithMultipleRootNodes_ShouldHandleMultipleRoots() {
        // Given: Two independent workflows (node1 and node2 are both roots)
        WorkflowDescription description = createSimpleWorkflowDescription();

        WorkflowNodeDescription node1 = createNodeDescription(WorkflowNodeType.START, "start:test.start1",
                List.of("task1"));
        WorkflowNodeDescription node2 = createNodeDescription(WorkflowNodeType.START, "start:test.start2",
                List.of("task2"));
        WorkflowNodeDescription task1 = createNodeDescription(WorkflowNodeType.TASK, "task:test.task1",
                new ArrayList<>());
        WorkflowNodeDescription task2 = createNodeDescription(WorkflowNodeType.TASK, "task:test.task2",
                new ArrayList<>());

        Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
        nodes.put("start1", node1);
        nodes.put("start2", node2);
        nodes.put("task1", task1);
        nodes.put("task2", task2);
        description.setNodes(nodes);

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then
        assertNotNull(workflow);
        assertEquals(2, workflow.getNodes().size()); // Two root nodes
    }

    @Test
    void toWorkflowDescription_WithValidWorkflow_ShouldTransformCorrectly() {
        // Given
        Workflow workflow = createSimpleWorkflow();

        // When
        WorkflowDescription description = transformer.toWorkflowDescription(workflow);

        // Then
        assertNotNull(description);
        assertEquals(workflow.getName(), description.getName());
        assertEquals(workflow.getDomain(), description.getDomain());
        assertEquals(workflow.getKey(), description.getKey());
        assertEquals(workflow.getDescription(), description.getDescription());
        assertEquals(workflow.getVersion(), description.getVersion());
        assertEquals(workflow.isActive(), description.isActive());
    }

    @Test
    void toWorkflowDescription_WithNullWorkflow_ShouldReturnNull() {
        // When
        WorkflowDescription description = transformer.toWorkflowDescription(null);

        // Then
        assertNull(description);
    }

    @Test
    void toWorkflowDescription_WithSimpleNodeHierarchy_ShouldFlattenCorrectly() {
        // Given: start -> task -> end
        WorkflowNode endNode = WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .component("end:test.end")
                .workflowNodeKey("end1")
                .build();

        WorkflowNode taskNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("task1")
                .addChild(endNode)
                .build();

        WorkflowNode startNode = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .component("start:test.start")
                .workflowNodeKey("start1")
                .addChild(taskNode)
                .build();

        Workflow workflow = createSimpleWorkflow();
        workflow.setNodes(List.of(startNode));

        // When
        WorkflowDescription description = transformer.toWorkflowDescription(workflow);

        // Then
        assertNotNull(description);
        assertNotNull(description.getNodes());
        assertEquals(3, description.getNodes().size());

        // All nodes should be in the map
        assertTrue(description.getNodes().containsKey("start1"));
        assertTrue(description.getNodes().containsKey("task1"));
        assertTrue(description.getNodes().containsKey("end1"));

        // Verify relationships
        WorkflowNodeDescription startDesc = description.getNodes().get("start1");
        assertEquals(1, startDesc.getNext().size());
        assertTrue(startDesc.getNext().contains("task1"));

        WorkflowNodeDescription taskDesc = description.getNodes().get("task1");
        assertEquals(1, taskDesc.getNext().size());
        assertTrue(taskDesc.getNext().contains("end1"));

        WorkflowNodeDescription endDesc = description.getNodes().get("end1");
        assertTrue(endDesc.getNext().isEmpty());
    }

    @Test
    void toWorkflowDescription_WithComplexNodeHierarchy_ShouldFlattenCorrectly() {
        // Given: start -> decision -> (task1, task2) -> end
        WorkflowNode endNode = WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .component("end:test.end")
                .workflowNodeKey("end1")
                .build();

        WorkflowNode task1 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task1")
                .workflowNodeKey("task1")
                .addChild(endNode)
                .build();

        WorkflowNode task2 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task2")
                .workflowNodeKey("task2")
                .addChild(endNode)
                .build();

        WorkflowNode decision = WorkflowNode.builder()
                .type(WorkflowNodeType.DECISION)
                .component("decision:test.decision")
                .workflowNodeKey("decision1")
                .addChild(task1)
                .addChild(task2)
                .build();

        WorkflowNode start = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .component("start:test.start")
                .workflowNodeKey("start1")
                .addChild(decision)
                .build();

        Workflow workflow = createSimpleWorkflow();
        workflow.setNodes(List.of(start));

        // When
        WorkflowDescription description = transformer.toWorkflowDescription(workflow);

        // Then
        assertNotNull(description);
        assertEquals(5, description.getNodes().size());

        WorkflowNodeDescription decisionDesc = description.getNodes().get("decision1");
        assertEquals(2, decisionDesc.getNext().size());
        assertTrue(decisionDesc.getNext().contains("task1"));
        assertTrue(decisionDesc.getNext().contains("task2"));

        // Both tasks should point to end
        WorkflowNodeDescription task1Desc = description.getNodes().get("task1");
        assertEquals(1, task1Desc.getNext().size());
        assertTrue(task1Desc.getNext().contains("end1"));

        WorkflowNodeDescription task2Desc = description.getNodes().get("task2");
        assertEquals(1, task2Desc.getNext().size());
        assertTrue(task2Desc.getNext().contains("end1"));
    }

    @Test
    void toWorkflowDescription_WithNodesHavingComponentConfig_ShouldPreserveConfig() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("task1")
                .componentConfig(ComponentConfig.builder()
                        .put("address", "test@example.com")
                        .put("retryCount", 3)
                        .build())
                .build();

        Workflow workflow = createSimpleWorkflow();
        workflow.setNodes(List.of(node));

        // When
        WorkflowDescription description = transformer.toWorkflowDescription(workflow);

        // Then
        WorkflowNodeDescription nodeDesc = description.getNodes().get("task1");
        assertNotNull(nodeDesc.getConfig());
        assertEquals("test@example.com", nodeDesc.getConfig().get("address"));
        assertEquals(3, nodeDesc.getConfig().get("retryCount"));
    }

    @Test
    void roundTripTransformation_ShouldPreserveWorkflowData() {
        // Given
        WorkflowDescription original = createSimpleWorkflowDescription();

        WorkflowNodeDescription startNode = createNodeDescription(WorkflowNodeType.START, "start:test.start",
                List.of("task1"));
        WorkflowNodeDescription taskNode = createNodeDescription(WorkflowNodeType.TASK, "task:test.task",
                List.of("end1"));
        WorkflowNodeDescription endNode = createNodeDescription(WorkflowNodeType.END, "end:test.end",
                new ArrayList<>());

        Map<String, Object> taskConfig = new HashMap<>();
        taskConfig.put("address", "test@example.com");
        taskNode.setConfig(taskConfig);

        Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
        nodes.put("start1", startNode);
        nodes.put("task1", taskNode);
        nodes.put("end1", endNode);
        original.setNodes(nodes);

        // When: Transform to workflow and back
        Workflow workflow = transformer.toWorkflow(original);
        WorkflowDescription result = transformer.toWorkflowDescription(workflow);

        // Then
        assertNotNull(result);
        assertEquals(original.getName(), result.getName());
        assertEquals(original.getDomain(), result.getDomain());
        assertEquals(original.getKey(), result.getKey());
        assertEquals(original.getDescription(), result.getDescription());
        assertEquals(original.getVersion(), result.getVersion());
        assertEquals(original.isActive(), result.isActive());
        assertEquals(3, result.getNodes().size());

        // Verify node relationships are preserved
        WorkflowNodeDescription resultStart = result.getNodes().get("start1");
        assertNotNull(resultStart);
        assertEquals(1, resultStart.getNext().size());
        assertTrue(resultStart.getNext().contains("task1"));

        WorkflowNodeDescription resultTask = result.getNodes().get("task1");
        assertNotNull(resultTask);
        assertEquals("test@example.com", resultTask.getConfig().get("address"));
    }

    @Test
    void toWorkflow_WithNodesMissingInNextField_ShouldNotCrash() {
        // Given: A node references a non-existent node in its "next" field
        WorkflowDescription description = createSimpleWorkflowDescription();

        WorkflowNodeDescription startNode = createNodeDescription(WorkflowNodeType.START, "start:test.start",
                List.of("nonExistentNode"));

        Map<String, WorkflowNodeDescription> nodes = new HashMap<>();
        nodes.put("start1", startNode);
        description.setNodes(nodes);

        // When
        Workflow workflow = transformer.toWorkflow(description);

        // Then: Should handle gracefully - non-existent node won't be added as child
        assertNotNull(workflow);
        assertEquals(1, workflow.getNodes().size());
        WorkflowNode root = workflow.getNodes().get(0);
        assertTrue(root.getChildren().isEmpty()); // No valid child added
    }

    @Test
    void toWorkflowDescription_WithEmptyNodesList_ShouldCreateEmptyMap() {
        // Given
        Workflow workflow = createSimpleWorkflow();
        workflow.setNodes(new ArrayList<>());

        // When
        WorkflowDescription description = transformer.toWorkflowDescription(workflow);

        // Then
        assertNotNull(description);
        assertNotNull(description.getNodes());
        assertTrue(description.getNodes().isEmpty());
    }

    // Helper methods

    private WorkflowDescription createSimpleWorkflowDescription() {
        WorkflowDescription description = new WorkflowDescription();
        description.setName("Test Workflow");
        description.setDomain("test.workflow");
        description.setKey("test-key");
        description.setDescription("A test workflow");
        description.setVersion(1);
        description.setActive(true);
        description.setNodes(new HashMap<>());
        return description;
    }

    private WorkflowNodeDescription createNodeDescription(WorkflowNodeType type, String component, List<String> next) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setType(type);
        if (component.contains(":")) {
            String[] parts = component.split(":", 2);
            node.setActor(parts[0]);
            node.setComponent(parts[1]);
        } else {
            node.setComponent(component);
        }
        node.setNext(next != null ? new ArrayList<>(next) : new ArrayList<>());
        node.setConfig(new HashMap<>());
        return node;
    }

    private Workflow createSimpleWorkflow() {
        return Workflow.builder("test")
                .withName("Test Workflow")
                .withDomain("test.workflow")
                .withKey("test-key")
                .withDescription("A test workflow")
                .withVersion(1)
                .withActive(true)
                .withNodes(new ArrayList<>())
                .build();
    }
}
