package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowGraphNode;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinComponentWaitModeTest {

    private JoinComponent joinComponent;
    private Context context;
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
            public WorkflowExecution getWorkflowExecution(String id) {
                return workflowExecution;
            }

            @Override
            public WorkflowExecution register(WorkflowExecution workflowExecution) {
                return workflowExecution;
            }
        };

        joinComponent = new JoinComponent(registry);
        context = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowExecutionId("testExecId");
        runtimeData.setWorkflowNodeId("testNodeId");
        runtimeData.setWorkflowNodeExecutionId("testNodeExecId");
        context.setRuntimeData(runtimeData);
        emitter = new TestEmitter();
    }

    private void configureComponent(List<String> awaits, String waitMode, Integer threshold) {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(JoinComponent.CONF_AWAIT, awaits);
        if (waitMode != null) {
            config.set(JoinComponent.CONF_WAIT_MODE, waitMode);
        }
        if (threshold != null) {
            config.set(JoinComponent.CONF_THRESHOLD_CT, threshold);
        }
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

    @Test
    void testWaitMode_ANY_FirstArrival() {
        configureComponent(List.of("A", "B"), "ANY", null);

        // A arrives
        addNodeToContext(context, "A", Collections.emptyList());
        registerExecution(context);

        joinComponent.run(context, emitter);
        assertTrue(emitter.successCalled, "Should succeed when A arrives in ANY mode");
    }

    @Test
    void testWaitMode_ANY_SecondArrival_Skips() {
        configureComponent(List.of("A", "B"), "ANY", null);

        // A arrives first (simulated by existing execution)
        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("testExecId");
        runtimeDataA.setWorkflowNodeId("testNodeId");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);

        // B arrives later
        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("testExecId");
        runtimeDataB.setWorkflowNodeId("testNodeId");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);
        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);

        // We are processing B now.
        // Since A (execA) already satisfied the condition (ANY), and execA < execB,
        // execA is the trigger. execB should skip.
        joinComponent.run(contextB, emitter);

        assertFalse(emitter.successCalled, "Should skip (not emit) when A already satisfied ANY mode");
    }

    @Test
    void testWaitMode_THRESHOLD_NotMet() {
        configureComponent(List.of("A", "B", "C"), "THRESHOLD", 2);

        // A arrives
        addNodeToContext(context, "A", Collections.emptyList());
        registerExecution(context);

        joinComponent.run(context, emitter);
        assertFalse(emitter.successCalled, "Should NOT succeed when only 1/3 arrived with threshold 2");
    }

    @Test
    void testWaitMode_THRESHOLD_Met() {
        configureComponent(List.of("A", "B", "C"), "THRESHOLD", 2);

        // A arrives
        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("testExecId");
        runtimeDataA.setWorkflowNodeId("testNodeId");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);

        // B arrives
        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("testExecId");
        runtimeDataB.setWorkflowNodeId("testNodeId");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);
        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);

        joinComponent.run(contextB, emitter);
        assertTrue(emitter.successCalled, "Should succeed when 2/3 arrived with threshold 2");
    }

    @Test
    void testWaitMode_THRESHOLD_SecondArrival_Skips() {
        configureComponent(List.of("A", "B", "C"), "THRESHOLD", 2);

        // A arrives
        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("testExecId");
        runtimeDataA.setWorkflowNodeId("testNodeId");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);

        // B arrives
        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("testExecId");
        runtimeDataB.setWorkflowNodeId("testNodeId");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);
        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);

        // C arrives
        Context contextC = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataC = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataC.setWorkflowExecutionId("testExecId");
        runtimeDataC.setWorkflowNodeId("testNodeId");
        runtimeDataC.setWorkflowNodeExecutionId("execC");
        contextC.setRuntimeData(runtimeDataC);
        addNodeToContext(contextC, "C", Collections.emptyList());
        registerExecution(contextC);

        // We are processing C now.
        // Threshold is 2.
        // A (execA) -> {A} (1)
        // B (execB) -> {A, B} (2) -> MET. Trigger = execB.
        // C (execC) -> {A, B, C} (3).
        // Trigger was execB. execC > execB.
        // execC should skip.

        joinComponent.run(contextC, emitter);
        assertFalse(emitter.successCalled, "Should skip when threshold was already met by previous execution");
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
        public Logger logger() {
            return (message, level, throwable) -> {
            };
        }
    }
}
