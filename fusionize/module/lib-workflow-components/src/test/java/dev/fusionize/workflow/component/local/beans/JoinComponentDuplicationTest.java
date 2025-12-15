package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import dev.fusionize.workflow.context.WorkflowGraphNode;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinComponentDuplicationTest {

    private JoinComponent joinComponent;
    private TestEmitter emitter;
    private WorkflowExecutionRegistry registry;
    private WorkflowExecution workflowExecution;
    private List<WorkflowNodeExecution> nodeExecutions;

    @BeforeEach
    void setUp() {
        nodeExecutions = new ArrayList<>();
        workflowExecution = new WorkflowExecution() {
            @Override
            public List<WorkflowNodeExecution> findNodesByWorkflowNodeId(String workflowNodeId) {
                return new ArrayList<>(nodeExecutions);
            }
        };

        registry = new WorkflowExecutionRegistry() {
            @Override
            public List<WorkflowExecution> getWorkflowExecutions(String workflowId) {
                return List.of();
            }

            @Override
            public WorkflowExecution getWorkflowExecution(String id) {
                return workflowExecution;
            }

            @Override
            public WorkflowExecution register(WorkflowExecution workflowExecution) {
                return workflowExecution;
            }

            @Override
            public void updateNodeExecution(String workflowExecutionId, WorkflowNodeExecution nodeExecution) {

            }

            @Override
            public void updateStatus(String workflowExecutionId, WorkflowExecutionStatus status) {

            }
        };

        joinComponent = new JoinComponent(registry);
        emitter = new TestEmitter();
    }

    @Test
    void testRun_MergesDuplicateDecisionsAndNodes() {
        configureComponent(List.of("B", "C"));

        // Diamond shape: A -> B, A -> C, (B, C) -> Join

        // Common ancestor A
        WorkflowGraphNode nodeA = new WorkflowGraphNode();
        nodeA.setNode("A");
        nodeA.setState(WorkflowNodeExecutionState.DONE);
        nodeA.setParents(Collections.emptyList());

        WorkflowDecision decisionA = new WorkflowDecision();
        decisionA.setDecisionNode("A");
        decisionA.setOptionNodes(Map.of("B", true, "C", true));

        // Context B (History: A, B)
        Context contextB = new Context();
        setupRuntimeData(contextB, "execB");
        contextB.getGraphNodes().add(nodeA); // A is in history
        contextB.getDecisions().add(decisionA); // A's decision is in history
        addNodeToContext(contextB, "B", List.of("A"));
        registerExecution(contextB);

        // Context C (History: A, C)
        Context contextC = new Context();
        setupRuntimeData(contextC, "execC");
        contextC.getGraphNodes().add(nodeA); // A is in history (Duplicate!)
        contextC.getDecisions().add(decisionA); // A's decision is in history (Duplicate!)
        addNodeToContext(contextC, "C", List.of("A"));
        registerExecution(contextC);

        // Add Join Node to Context B (Parent: B)
        WorkflowGraphNode joinNodeB = new WorkflowGraphNode();
        joinNodeB.setNode("JoinNode");
        joinNodeB.setParents(List.of("B"));
        joinNodeB.setState(WorkflowNodeExecutionState.WORKING);
        contextB.getGraphNodes().add(joinNodeB);

        // Add Join Node to Context C (Parent: C)
        WorkflowGraphNode joinNodeC = new WorkflowGraphNode();
        joinNodeC.setNode("JoinNode");
        joinNodeC.setParents(List.of("C"));
        joinNodeC.setState(WorkflowNodeExecutionState.WORKING);
        contextC.getGraphNodes().add(joinNodeC);

        // Run Join
        // First run with B (waits)
        joinComponent.run(contextB, emitter);
        // Second run with C (completes)
        joinComponent.run(contextC, emitter);

        assertTrue(emitter.successCalled);
        Context result = emitter.capturedContext;

        // Verify Duplication
        // We expect A to be present only once if fixed, but currently it will be twice.
        // Decisions: A should be present once.

        long countNodeA = result.getGraphNodes().stream().filter(n -> "A".equals(n.getNode())).count();
        long countDecisionA = result.getDecisions().stream().filter(d -> "A".equals(d.getDecisionNode())).count();

        System.out.println("Count Node A: " + countNodeA);
        System.out.println("Count Decision A: " + countDecisionA);

        // Asserting the CORRECT behavior after fix
        assertEquals(1, countNodeA, "Should have NO duplicate Node A after fix");
        assertEquals(1, countDecisionA, "Should have NO duplicate Decision A after fix");

        // Verify Parent Merging for Join Node
        List<WorkflowGraphNode> joinNodes = result.getGraphNodes().stream()
                .filter(n -> "JoinNode".equals(n.getNode())).toList();
        assertEquals(1, joinNodes.size());
        WorkflowGraphNode mergedJoinNode =  joinNodes.getFirst();

        assertTrue(mergedJoinNode.getParents().contains("B"), "JoinNode should have parent B");
        assertTrue(mergedJoinNode.getParents().contains("C"), "JoinNode should have parent C");
        assertEquals(2, mergedJoinNode.getParents().size(), "JoinNode should have exactly 2 parents");
    }

    private void setupRuntimeData(Context ctx, String nodeExecId) {
        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowExecutionId("exec1");
        runtimeData.setWorkflowNodeId("join1");
        runtimeData.setWorkflowNodeExecutionId(nodeExecId);
        ctx.setRuntimeData(runtimeData);
    }

    private void configureComponent(List<String> awaits) {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(JoinComponent.CONF_AWAIT, awaits);
        joinComponent.configure(config);
    }

    private void addNodeToContext(Context ctx, String nodeId, List<String> parentIds) {
        WorkflowGraphNode node = new WorkflowGraphNode();
        node.setNode(nodeId);
        node.setParents(parentIds);
        node.setState(WorkflowNodeExecutionState.DONE);
        ctx.getGraphNodes().add(node);
    }

    private void registerExecution(Context ctx) {
        WorkflowNodeExecution ne = new WorkflowNodeExecution();
        ne.setStageContext(ctx);
        ne.setWorkflowNodeExecutionId(ctx.getRuntimeData().getWorkflowNodeExecutionId());
        ne.setWorkflowNodeId(ctx.getRuntimeData().getWorkflowNodeId());
        nodeExecutions.add(ne);
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        Context capturedContext;

        @Override
        public void success(Context updatedContext) {
            successCalled = true;
            capturedContext = updatedContext;
        }

        @Override
        public void failure(Exception ex) {
            throw new RuntimeException(ex);
        }

        @Override
        public ComponentUpdateEmitter.Logger logger() {
            return (message, level, throwable) -> {
            };
        }
    }
}
