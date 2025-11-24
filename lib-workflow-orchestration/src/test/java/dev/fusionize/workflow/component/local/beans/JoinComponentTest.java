package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowGraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinComponentTest {

    private JoinComponent joinComponent;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        joinComponent = new JoinComponent();
        context = new Context();
        context.set("_executionId", "testExecId");
        context.set("_nodeId", "testNodeId");
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

    @Test
    void testRun_AllAwaitedNodesPresent() {
        configureComponent(List.of("A", "B"));
        addNodeToContext(context, "A", Collections.emptyList());
        addNodeToContext(context, "B", Collections.emptyList());

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

        joinComponent.run(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void testRun_WaitUntilAllPresent() {
        configureComponent(List.of("A", "B"));

        // First call with Context A (missing B)
        Context contextA = new Context();
        contextA.set("_executionId", "exec1");
        contextA.set("_nodeId", "join1");
        contextA.set("varA", "valA");
        addNodeToContext(contextA, "A", Collections.emptyList());

        joinComponent.run(contextA, emitter);
        assertFalse(emitter.successCalled);

        // Second call with Context B (missing A)
        Context contextB = new Context();
        contextB.set("_executionId", "exec1");
        contextB.set("_nodeId", "join1");
        contextB.set("varB", "valB");
        addNodeToContext(contextB, "B", Collections.emptyList());

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
        contextA.set("_executionId", "exec2");
        contextA.set("_nodeId", "join2");
        contextA.set("shared", "valA");
        addNodeToContext(contextA, "A", Collections.emptyList());

        joinComponent.run(contextA, emitter);

        Context contextB = new Context();
        contextB.set("_executionId", "exec2");
        contextB.set("_nodeId", "join2");
        contextB.set("shared", "valB");
        addNodeToContext(contextB, "B", Collections.emptyList());

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
    }
}
