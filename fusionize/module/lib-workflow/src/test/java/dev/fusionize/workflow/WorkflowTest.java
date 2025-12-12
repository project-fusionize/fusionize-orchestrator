package dev.fusionize.workflow;

import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
    
    @Test
    void mergeFrom_ShouldPreserveId_WhenNodeUnchanged() {
        // Create initial node
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .component("some-component")
                .build();
        String originalNodeId = node.getWorkflowNodeId();
        
        Workflow original = Workflow.builder("test")
                .addNode(node)
                .build();
        // Mimic existing structure where nodeMap might be populated
        original.getNodeMap().put(node.getWorkflowNodeKey(), node);

        // Create update with identical node content (but different instance)
        WorkflowNode updateNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .component("some-component")
                .build();
        // Ideally this has a new ID initially
        assertNotEquals(originalNodeId, updateNode.getWorkflowNodeId());

        Workflow update = Workflow.builder("test")
                .addNode(updateNode)
                .build();

        original.mergeFrom(update);

        // Should have preserved the original ID
        assertEquals(1, original.getNodes().size());
        assertEquals(originalNodeId, original.getNodes().get(0).getWorkflowNodeId());
    }

    @Test
    void mergeFrom_ShouldUpdateId_WhenNodeChanged() {
        // Create initial node
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .component("component-v1")
                .build();
        String originalNodeId = node.getWorkflowNodeId();
        
        Workflow original = Workflow.builder("test")
                .addNode(node)
                .build();
        original.getNodeMap().put(node.getWorkflowNodeKey(), node);

        // Create update with changed component
        WorkflowNode updateNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .component("component-v2")
                .build();
        
        Workflow update = Workflow.builder("test")
                .addNode(updateNode)
                .build();

        original.mergeFrom(update);

        assertEquals(1, original.getNodes().size());
        // ID should be the new one (from updateNode)
        assertEquals(updateNode.getWorkflowNodeId(), original.getNodes().get(0).getWorkflowNodeId());
        assertNotEquals(originalNodeId, original.getNodes().get(0).getWorkflowNodeId());
    }

    @Test
    void mergeFrom_ShouldPreserveId_Recursive() {
         // Parent node
         WorkflowNode child = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("child")
                .component("child-comp")
                .build();
         WorkflowNode parent = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .workflowNodeKey("parent")
                .addChild(child)
                .build();
        
        String parentId = parent.getWorkflowNodeId();
        String childId = child.getWorkflowNodeId();

        Workflow original = Workflow.builder("test")
                .addNode(parent)
                .build();
        // Flatten map needs to be consistent for the test setup if we rely on map in mergeFrom
        // The implementation falls back to flattening `nodes` if `nodeMap` is empty, so we don't strictly need to populate map here manually
        // but for completeness let's rely on the fallback logic
        
        // Create identical update structure
         WorkflowNode childUpdate = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("child")
                .component("child-comp")
                .build();
         WorkflowNode parentUpdate = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .workflowNodeKey("parent")
                .addChild(childUpdate)
                .build();

        Workflow update = Workflow.builder("test")
                .addNode(parentUpdate)
                .build();

        original.mergeFrom(update);

        assertEquals(parentId, original.getNodes().get(0).getWorkflowNodeId());
        assertEquals(childId, original.getNodes().get(0).getChildren().get(0).getWorkflowNodeId());
    }
    
    @Test
    void mergeFrom_ShouldHandleRemovedNodes() {
        WorkflowNode node1 = WorkflowNode.builder().workflowNodeKey("n1").build();
        WorkflowNode node2 = WorkflowNode.builder().workflowNodeKey("n2").build();
        
        Workflow original = Workflow.builder("test")
                .addNode(node1)
                .addNode(node2)
                .build();
        
        // Update only has n1
        WorkflowNode node1Update = WorkflowNode.builder().workflowNodeKey("n1").build();
        Workflow update = Workflow.builder("test")
                .addNode(node1Update)
                .build();
        
        original.mergeFrom(update);
        
        assertEquals(1, original.getNodes().size());
        assertEquals("n1", original.getNodes().get(0).getWorkflowNodeKey());
    }
}
