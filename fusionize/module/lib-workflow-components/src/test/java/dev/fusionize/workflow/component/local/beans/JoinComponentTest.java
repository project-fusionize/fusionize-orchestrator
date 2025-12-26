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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinComponentTest {

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
        context = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowExecutionId("testExecId");
        runtimeData.setWorkflowNodeId("testNodeId");
        runtimeData.setWorkflowNodeExecutionId("testNodeExecId");
        context.setRuntimeData(runtimeData);
        emitter = new TestEmitter();
    }

    @Test
    void testCanActivate_EmptyAwaits() {
        configureComponent(Collections.emptyList());
        joinComponent.canActivate(context, emitter);
        assertFalse(emitter.successCalled);
    }

    @Test
    void testCanActivate_AwaitedNodeNotPresent() {
        configureComponent(List.of("A"));
        addNodeToContext(context, "B", Collections.emptyList());

        joinComponent.canActivate(context, emitter);
        assertFalse(emitter.successCalled);
    }

    @Test
    void testCanActivate_AwaitedNodePresentDirectly() {
        configureComponent(List.of("A"));
        addNodeToContext(context, "A", Collections.emptyList());

        joinComponent.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void testCanActivate_AwaitedNodePresentInAncestry() {
        configureComponent(List.of("A"));
        // A -> B
        addNodeToContext(context, "A", Collections.emptyList());
        addNodeToContext(context, "B", List.of("A"));

        joinComponent.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void testRun_EmptyAwaits() {
        configureComponent(Collections.emptyList());
        joinComponent.run(context, emitter);
        assertFalse(emitter.successCalled);
    }

    @Test
    void testRun_NotAllAwaitedNodesPresent() {
        configureComponent(List.of("A", "B"));
        addNodeToContext(context, "A", Collections.emptyList());

        joinComponent.run(context, emitter);
        assertFalse(emitter.successCalled);
    }

    private void registerExecution(Context ctx) {
        WorkflowNodeExecution ne = new WorkflowNodeExecution();
        ne.setStageContext(ctx);
        ne.setWorkflowNodeExecutionId(ctx.getRuntimeData().getWorkflowNodeExecutionId());
        ne.setWorkflowNodeId(ctx.getRuntimeData().getWorkflowNodeId());
        nodeExecutions.add(ne);
    }

    @Test
    void testRun_AllAwaitedNodesPresent() {
        configureComponent(List.of("A", "B"));
        addNodeToContext(context, "A", Collections.emptyList());
        addNodeToContext(context, "B", Collections.emptyList());
        registerExecution(context);

        joinComponent.run(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void testRun_AllAwaitedNodesPresentInAncestry() {
        configureComponent(List.of("A", "B"));
        // A -> C
        // B -> D
        addNodeToContext(context, "A", Collections.emptyList());
        addNodeToContext(context, "B", Collections.emptyList());
        addNodeToContext(context, "C", List.of("A"));
        addNodeToContext(context, "D", List.of("B"));
        registerExecution(context);

        joinComponent.run(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void testRun_WaitUntilAllPresent() {
        configureComponent(List.of("A", "B"));

        // First call with Context A (missing B)
        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("exec1");
        runtimeDataA.setWorkflowNodeId("join1");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        contextA.set("varA", "valA");
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);

        joinComponent.run(contextA, emitter);
        assertFalse(emitter.successCalled);

        // Second call with Context B (missing A)
        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("exec1");
        runtimeDataB.setWorkflowNodeId("join1");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);
        contextB.set("varB", "valB");
        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);

        joinComponent.run(contextB, emitter);
        assertTrue(emitter.successCalled);

        // Verify merged context
        Context result = emitter.capturedContext;
        assertTrue(result.contains("varA"));
        assertTrue(result.contains("varB"));
    }

    @Test
    void testRun_MergeStrategy_PickLast() {
        configureComponent(List.of("A", "B")); // Default is PICK_LAST

        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("exec2");
        runtimeDataA.setWorkflowNodeId("join2");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        contextA.set("shared", "valA");
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);

        joinComponent.run(contextA, emitter);

        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("exec2");
        runtimeDataB.setWorkflowNodeId("join2");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);
        contextB.set("shared", "valB");
        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);

        joinComponent.run(contextB, emitter);
        assertTrue(emitter.successCalled);

        assertEquals("valB", emitter.capturedContext.varString("shared").orElse(null));
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
            // Fail test if failure called unexpectedly
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

    @Test
    void testRun_CircularDependency_IgnoresPreviousCycle() {
        String joinNodeId = "joinNode";
        String awaitedNodeId = "awaitedNode";

        configureComponent(List.of(awaitedNodeId));

        Context ctx = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowExecutionId("exec-circular");
        runtimeData.setWorkflowNodeId(joinNodeId);
        runtimeData.setWorkflowNodeExecutionId("exec-2");
        ctx.setRuntimeData(runtimeData);

        // Build Graph
        // Awaited Node (from previous cycle)
        addNodeToContext(ctx, awaitedNodeId, Collections.emptyList());
        // Join Node (previous execution) -> depends on Awaited Node
        addNodeToContext(ctx, joinNodeId, List.of(awaitedNodeId));
        // Intermediate Node -> depends on Join Node
        addNodeToContext(ctx, "intermediate", List.of(joinNodeId));

        registerExecution(ctx);

        joinComponent.run(ctx, emitter);

        assertFalse(emitter.successCalled,
                "Should not succeed because awaited node is behind the previous execution of the join node");
    }
    @Test
    void testDeepMerge_NestedMapsAndLists() {
        configureComponent(List.of("A", "B")); // Default is PICK_LAST

        // Context A: { nested: { a: 1 }, list: [x] }
        Context contextA = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataA = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataA.setWorkflowExecutionId("execDeep");
        runtimeDataA.setWorkflowNodeId("joinDeep");
        runtimeDataA.setWorkflowNodeExecutionId("execA");
        contextA.setRuntimeData(runtimeDataA);
        
        java.util.Map<String, Object> nestedA = new java.util.HashMap<>();
        nestedA.put("a", 1);
        contextA.set("nested", nestedA);
        
        List<Object> listA = new ArrayList<>();
        listA.add("x");
        contextA.set("list", listA);
        
        addNodeToContext(contextA, "A", Collections.emptyList());
        registerExecution(contextA);
        joinComponent.run(contextA, emitter);

        // Context B: { nested: { b: 2 }, list: [x, y] }
        Context contextB = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeDataB = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeDataB.setWorkflowExecutionId("execDeep");
        runtimeDataB.setWorkflowNodeId("joinDeep");
        runtimeDataB.setWorkflowNodeExecutionId("execB");
        contextB.setRuntimeData(runtimeDataB);

        java.util.Map<String, Object> nestedB = new java.util.HashMap<>();
        nestedB.put("b", 2);
        contextB.set("nested", nestedB);

        List<Object> listB = new ArrayList<>();
        listB.add("x");
        listB.add("y");
        contextB.set("list", listB);

        addNodeToContext(contextB, "B", Collections.emptyList());
        registerExecution(contextB);
        joinComponent.run(contextB, emitter);

        assertTrue(emitter.successCalled);

        // Verification
        Context result = emitter.capturedContext;
        
        // Check Nested Map Merge: { a: 1, b: 2 }
        java.util.Map<String, Object> resultNested = (java.util.Map<String, Object>) result.getData().get("nested");
        assertEquals(1, resultNested.get("a"));
        assertEquals(2, resultNested.get("b"));

        // Check List Merge Deduplication: [x, y] (order matter for list, set for unique)
        List<Object> resultList = (List<Object>) result.getData().get("list");
        assertEquals(2, resultList.size());
        assertTrue(resultList.contains("x"));
        assertTrue(resultList.contains("y"));
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

        TestEmitterAtomic emitter = new TestEmitterAtomic();

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

    static class TestEmitterAtomic implements ComponentUpdateEmitter {
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
