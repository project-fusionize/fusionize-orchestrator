package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrchestrationDecisionEngineTest {

    private OrchestratorDecisionEngine decisionEngine;

    @BeforeEach
    public void setUp() {
        decisionEngine = new OrchestratorDecisionEngine();
    }

    @Test
    public void testDetermineNextNodes_NoDecision() {
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .build();
        WorkflowNode child1 = WorkflowNode.builder().workflowNodeId("child1").build();
        node.setChildren(new ArrayList<>(List.of(child1)));

        WorkflowNodeExecution execution = WorkflowNodeExecution.of(node, Context.builder().build());

        List<WorkflowNode> nextNodes = decisionEngine.determineNextNodes(execution);

        assertEquals(1, nextNodes.size());
        assertEquals("child1", nextNodes.get(0).getWorkflowNodeId());
    }

    @Test
    public void testDetermineNextNodes_Decision() {
        WorkflowNode decisionNode = WorkflowNode.builder()
                .type(WorkflowNodeType.DECISION)
                .workflowNodeKey("decision1")
                .build();
        
        WorkflowNode optionA = WorkflowNode.builder().workflowNodeId("A").workflowNodeKey("optionA").build();
        WorkflowNode optionB = WorkflowNode.builder().workflowNodeId("B").workflowNodeKey("optionB").build();
        
        decisionNode.setChildren(new ArrayList<>(List.of(optionA, optionB)));

        Context context = Context.builder().build();
        Map<String, Boolean> options = new HashMap<>();
        options.put("optionA", true);
        options.put("optionB", false);
        
        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("decision1");
        decision.setOptionNodes(options);
        
        context.getDecisions().add(decision);

        WorkflowNodeExecution execution = WorkflowNodeExecution.of(decisionNode, context);

        List<WorkflowNode> nextNodes = decisionEngine.determineNextNodes(execution);

        assertEquals(1, nextNodes.size());
        assertEquals("A", nextNodes.get(0).getWorkflowNodeId());
    }
}
