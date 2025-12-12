package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.repo.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WorkflowRepoRegistryTest {

    private WorkflowRepository repository;
    private WorkflowRepoRegistry registry;

    @BeforeEach
    public void setUp() {
        repository = Mockito.mock(WorkflowRepository.class);
        registry = new WorkflowRepoRegistry(repository);
    }

    @Test
    public void testRegisterFlattensAndInflates() {
        // Create nodes with circular reference
        WorkflowNode nodeA = WorkflowNode.builder().workflowNodeId("A").build();
        WorkflowNode nodeB = WorkflowNode.builder().workflowNodeId("B").build();
        nodeA.setChildren(new ArrayList<>(List.of(nodeB)));
        nodeB.setChildren(new ArrayList<>(List.of(nodeA)));

        Workflow workflow = Workflow.builder("test-domain")
                .withWorkflowId("test-workflow")
                .addNode(nodeA)
                .build();

        // Mock save to return the same instance (simulating persistence)
        when(repository.save(any(Workflow.class))).thenAnswer(invocation -> {
            Workflow w = invocation.getArgument(0);
            // Verify it is flattened when passed to save
            assertFalse(w.getNodeMap().isEmpty());
            assertTrue(w.getRootNodeIds().contains("A"));
            // Simulate DB saving state (transient fields might be lost, but here we just return the object)
            return w;
        });

        Workflow result = registry.register(workflow);

        // Verify result is inflated
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals("A", result.getNodes().get(0).getWorkflowNodeId());
        assertEquals("B", result.getNodes().get(0).getChildren().get(0).getWorkflowNodeId());
        assertEquals("A", result.getNodes().get(0).getChildren().get(0).getChildren().get(0).getWorkflowNodeId());
    }

    @Test
    public void testGetWorkflowInflates() {
        // Create a flattened workflow (simulating DB state)
        Workflow flattenedWorkflow = Workflow.builder("test-domain")
                .withWorkflowId("test-workflow")
                .build();
        
        WorkflowNode nodeA = WorkflowNode.builder().workflowNodeId("A").build();
        nodeA.setChildrenIds(List.of("B"));
        WorkflowNode nodeB = WorkflowNode.builder().workflowNodeId("B").build();
        nodeB.setChildrenIds(List.of("A"));

        flattenedWorkflow.getNodeMap().put("A", nodeA);
        flattenedWorkflow.getNodeMap().put("B", nodeB);
        flattenedWorkflow.getRootNodeIds().add("A");

        when(repository.findByWorkflowId("test-workflow")).thenReturn(Optional.of(flattenedWorkflow));

        Workflow result = registry.getWorkflow("test-workflow");

        // Verify result is inflated
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals("A", result.getNodes().get(0).getWorkflowNodeId());
        assertEquals("B", result.getNodes().get(0).getChildren().get(0).getWorkflowNodeId());
        assertEquals("A", result.getNodes().get(0).getChildren().get(0).getChildren().get(0).getWorkflowNodeId());
    }
}
