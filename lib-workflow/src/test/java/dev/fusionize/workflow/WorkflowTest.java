package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTest {

    @Test
    void builder_ShouldCreateWorkflowWithDefaults() {
        Workflow workflow = Workflow.builder("test")
                .withName("Test Workflow")
                .build();

        assertNotNull(workflow);

        assertEquals("Test Workflow", workflow.getName());
        assertEquals("test", dev.fusionize.user.activity.DomainEntity.parent(workflow));
        assertTrue(workflow.isActive());
        assertNotNull(workflow.getWorkflowId());
        assertTrue(workflow.getNodes().isEmpty());
    }

    @Test
    void builder_ShouldAddNodes() {
        WorkflowNode node1 = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .workflowNodeKey("start")
                .build();

        WorkflowNode node2 = WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .workflowNodeKey("end")
                .build();

        Workflow workflow = Workflow.builder("test")
                .addNode(node1)
                .addNode(node2)
                .build();

        assertEquals(2, workflow.getNodes().size());
        assertTrue(workflow.getNodes().contains(node1));
        assertTrue(workflow.getNodes().contains(node2));
    }

    @Test
    void findNode_ShouldReturnNodeById() {
        WorkflowNode child = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("child")
                .workflowNodeId("child-id")
                .build();

        WorkflowNode parent = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .workflowNodeKey("parent")
                .workflowNodeId("parent-id")
                .addChild(child)
                .build();

        Workflow workflow = Workflow.builder("test")
                .addNode(parent)
                .build();

        // Test finding root node
        WorkflowNode foundParent = workflow.findNode("parent-id");
        assertNotNull(foundParent);
        assertEquals("parent-id", foundParent.getWorkflowNodeId());

        // Test finding child node (recursive search)
        WorkflowNode foundChild = workflow.findNode("child-id");
        assertNotNull(foundChild);
        assertEquals("child-id", foundChild.getWorkflowNodeId());

        // Test finding non-existent node
        assertNull(workflow.findNode("non-existent"));
    }

    @Test
    void findNode_WithNodeMap_ShouldReturnFromMap() {
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task")
                .build();

        Workflow workflow = new Workflow();
        workflow.setNodeMap(Map.of("task", node));

        WorkflowNode found = workflow.findNode("task");
        assertNotNull(found);
        assertEquals("task", found.getWorkflowNodeKey());
    }

    @Test
    void mergeFrom_ShouldUpdateFields() {
        Workflow original = Workflow.builder("test")
                .withName("Original")
                .withDescription("Original Desc")
                .withVersion(1)
                .withActive(false)
                .build();

        Workflow update = Workflow.builder("test")
                .withName("Updated")
                .withDescription("Updated Desc")
                .withVersion(2)
                .withActive(true)
                .build();

        original.mergeFrom(update);

        assertEquals("Updated", original.getName());
        assertEquals("Updated Desc", original.getDescription());
        assertEquals(2, original.getVersion());
        assertTrue(original.isActive());
    }

    @Test
    void mergeFrom_WithNull_ShouldDoNothing() {
        Workflow original = Workflow.builder("test")
                .withName("Original")
                .build();

        original.mergeFrom(null);

        assertEquals("Original", original.getName());
    }

    @Test
    void mergeFrom_WithPartialUpdate_ShouldOnlyUpdatePresentFields() {
        Workflow original = Workflow.builder("test")
                .withName("Original")
                .withDescription("Original Desc")
                .withVersion(1)
                .build();

        Workflow update = new Workflow();
        update.setName("Updated");
        // Description and Version are null/0 in update

        original.mergeFrom(update);

        assertEquals("Updated", original.getName());
        assertEquals("Original Desc", original.getDescription());
        assertEquals(1, original.getVersion());
    }
}
