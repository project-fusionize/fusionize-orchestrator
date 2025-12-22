package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import dev.fusionize.workflow.context.WorkflowGraphNode;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JoinComponentReproductionTest {

    private JoinComponent joinComponent;
    private WorkflowExecutionRegistry registry;
    private WorkflowExecution workflowExecution;
    private List<WorkflowNodeExecution> nodeExecutions;

    @BeforeEach
    void setUp() {
        nodeExecutions = new ArrayList<>();
        workflowExecution = new WorkflowExecution() {
            @Override
            public List<WorkflowNodeExecution> findNodesByWorkflowNodeId(String workflowNodeId) {
                return new ArrayList<>(nodeExecutions); // Return copy to simulate concurrent access if needed
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
    }

    @Test
    void testMultipleEmissions() {
        // Configure to await A, B, C
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(JoinComponent.CONF_AWAIT, List.of("A", "B", "C"));
        joinComponent.configure(config);

        String executionId = "exec-1";
        String nodeId = "join-node";

        // Create 3 contexts representing 3 parents finishing
        Context ctxA = createContext(executionId, nodeId, "A");
        Context ctxB = createContext(executionId, nodeId, "B");
        Context ctxC = createContext(executionId, nodeId, "C");

        // Create 3 executions
        WorkflowNodeExecution execA = createExecution(ctxA);
        WorkflowNodeExecution execB = createExecution(ctxB);
        WorkflowNodeExecution execC = createExecution(ctxC);

        // Simulate Orchestrator adding them to the execution list
        nodeExecutions.add(execA);
        nodeExecutions.add(execB);
        nodeExecutions.add(execC);

        TestEmitter emitter = new TestEmitter();

        // Run JoinComponent for each execution (simulating 3 concurrent activations)
        // Since all 3 are in the list, each run will see all 3.

        joinComponent.run(ctxA, emitter);
        joinComponent.run(ctxB, emitter);
        joinComponent.run(ctxC, emitter);

        // Expectation: Currently it emits 3 times. We want 1 time.

        // This assertion confirms the FIX.
        assertEquals(1, emitter.successCount.get(), "Should emit 1 time (fix verified)");
    }

    private Context createContext(String executionId, String nodeId, String parentNodeId) {
        Context ctx = new Context();
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowExecutionId(executionId);
        runtimeData.setWorkflowNodeId(nodeId);
        ctx.setRuntimeData(runtimeData);

        WorkflowGraphNode node = new WorkflowGraphNode();
        node.setNode(parentNodeId);
        node.setState(WorkflowNodeExecutionState.DONE);
        ctx.getGraphNodes().add(node);

        return ctx;
    }

    private WorkflowNodeExecution createExecution(Context ctx) {
        WorkflowNodeExecution ne = new WorkflowNodeExecution();
        ne.setStageContext(ctx);
        String id = UUID.randomUUID().toString();
        ne.setWorkflowNodeExecutionId(id);
        ctx.getRuntimeData().setWorkflowNodeExecutionId(id);
        return ne;
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        AtomicInteger successCount = new AtomicInteger(0);

        @Override
        public void success(Context updatedContext) {
            successCount.incrementAndGet();
        }

        @Override
        public void failure(Exception ex) {
            throw new RuntimeException(ex);
        }

        @Override
        public Logger logger() {
            return (message, level, throwable) -> {
            };
        }

        @Override
        public InteractionLogger interactionLogger() {
            return null;
        }

    }
}
