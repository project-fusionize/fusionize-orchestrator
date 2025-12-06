package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextFactoryTest {

    @Test
    void empty_ShouldReturnEmptyContext() {
        Context context = ContextFactory.empty();
        assertNotNull(context);
        assertTrue(context.getGraphNodes().isEmpty());
        assertTrue(context.getDecisions().isEmpty());
    }

    @Test
    void from_WithNullLastExecution_ShouldCreateNewContext() {
        WorkflowNode nextNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .build();

        Context context = ContextFactory.from(new WorkflowNodeExecution(), nextNode);

        assertNotNull(context);
        assertEquals(1, context.getGraphNodes().size());
        WorkflowGraphNode graphNode = context.getGraphNodes().get(0);
        assertEquals("task1", graphNode.getNode());
        assertEquals(WorkflowNodeExecutionState.IDLE, graphNode.getState());
        assertTrue(graphNode.getParents().isEmpty());
    }

    @Test
    void from_WithLastExecution_ShouldLinkParents() {
        WorkflowNode parentNode = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .workflowNodeKey("start1")
                .build();

        WorkflowNodeExecution lastExecution = new WorkflowNodeExecution();
        lastExecution.setWorkflowNode(parentNode);

        WorkflowNode nextNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .build();

        Context context = ContextFactory.from(lastExecution, nextNode);

        assertNotNull(context);
        assertEquals(1, context.getGraphNodes().size());
        WorkflowGraphNode graphNode = context.getGraphNodes().get(0);
        assertEquals("task1", graphNode.getNode());
        assertEquals(1, graphNode.getParents().size());
        assertTrue(graphNode.getParents().contains("start1"));
    }

    @Test
    void from_WithDecisionNode_ShouldAddDecisionMetadata() {
        WorkflowNode child1 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("child1")
                .build();

        WorkflowNode child2 = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("child2")
                .build();

        WorkflowNode decisionNode = WorkflowNode.builder()
                .type(WorkflowNodeType.DECISION)
                .workflowNodeKey("decision1")
                .addChild(child1)
                .addChild(child2)
                .build();

        Context context = ContextFactory.from(new WorkflowNodeExecution(), decisionNode);

        assertNotNull(context);
        assertEquals(1, context.getDecisions().size());
        WorkflowDecision decision = context.getDecisions().get(0);
        assertEquals("decision1", decision.getDecisionNode());
        assertEquals(2, decision.getOptionNodes().size());
        assertTrue(decision.getOptionNodes().containsKey("child1"));
        assertTrue(decision.getOptionNodes().containsKey("child2"));
        assertFalse(decision.getOptionNodes().get("child1"));
        assertFalse(decision.getOptionNodes().get("child2"));
    }

    @Test
    void from_WithExistingGraphNode_ShouldUpdateState() {
        // Setup existing context with a graph node
        Context existingContext = new Context();
        WorkflowGraphNode existingGraphNode = new WorkflowGraphNode();
        existingGraphNode.setNode("task1");
        existingGraphNode.setState(WorkflowNodeExecutionState.WORKING);
        existingContext.getGraphNodes().add(existingGraphNode);

        WorkflowNodeExecution lastExecution = new WorkflowNodeExecution();
        lastExecution.setStageContext(existingContext);

        WorkflowNode nextNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .workflowNodeKey("task1")
                .build();

        // Execute
        Context context = ContextFactory.from(lastExecution, nextNode);

        // Verify
        assertNotNull(context);
        assertEquals(1, context.getGraphNodes().size());
        WorkflowGraphNode graphNode = context.getGraphNodes().get(0);
        assertEquals("task1", graphNode.getNode());
        // State should be reset to IDLE by addOrUpdateGraphNode
        assertEquals(WorkflowNodeExecutionState.IDLE, graphNode.getState());
    }
}
