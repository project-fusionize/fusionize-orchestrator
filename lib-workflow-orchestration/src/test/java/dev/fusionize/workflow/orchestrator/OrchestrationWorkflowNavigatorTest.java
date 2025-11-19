package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.context.WorkflowContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrchestrationWorkflowNavigatorTest {

    private OrchestratorDecisionEngine decisionEngine;
    private OrchestratorWorkflowNavigator navigator;

    @BeforeEach
    public void setUp() {
        decisionEngine = Mockito.mock(OrchestratorDecisionEngine.class);
        navigator = new OrchestratorWorkflowNavigator(decisionEngine);
    }

    @Test
    public void testNavigate_SimpleTransition() {
        WorkflowNode node = WorkflowNode.builder().workflowNodeId("node1").type(WorkflowNodeType.TASK).build();
        WorkflowNode child = WorkflowNode.builder().workflowNodeId("node2").type(WorkflowNodeType.TASK).build();
        node.setChildren(new ArrayList<>(List.of(child)));

        Workflow workflow = Workflow.builder("domain").addNode(node).build();
        WorkflowExecution we = WorkflowExecution.of(workflow);
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, WorkflowContext.builder().build());
        we.getNodes().add(ne);

        when(decisionEngine.determineNextNodes(any())).thenReturn(List.of(child));

        List<WorkflowNodeExecution> nextExecutions = navigator.navigate(we, ne);

        assertEquals(1, nextExecutions.size());
        assertEquals("node2", nextExecutions.get(0).getWorkflowNodeId());
        assertEquals(WorkflowNodeExecutionState.DONE, ne.getState());
    }

    @Test
    public void testNavigate_EndNode() {
        WorkflowNode node = WorkflowNode.builder().workflowNodeId("end").type(WorkflowNodeType.END).build();
        
        Workflow workflow = Workflow.builder("domain").addNode(node).build();
        WorkflowExecution we = WorkflowExecution.of(workflow);
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, WorkflowContext.builder().build());
        we.getNodes().add(ne);

        when(decisionEngine.determineNextNodes(any())).thenReturn(new ArrayList<>());

        List<WorkflowNodeExecution> nextExecutions = navigator.navigate(we, ne);

        assertTrue(nextExecutions.isEmpty());
        assertEquals(WorkflowExecutionStatus.SUCCESS, we.getStatus());
    }
}
