package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ForkComponentTest {

    private ForkComponent forkComponent;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        forkComponent = new ForkComponent();
        context = new Context();

        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowNodeKey("forkNode");
        context.setRuntimeData(runtimeData);

        emitter = new TestEmitter();
    }

    // ────────────────────────────────────────────────
    // JS ENGINE TEST
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_JS() {
        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "value > 10");
        routeMap.put("routeB", "value < 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "js");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
    }

    // ────────────────────────────────────────────────
    // SPEL TEST — EXPLICIT `parser: spel`
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_SpEL_Explicit() {

        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "#value > 10");
        routeMap.put("routeB", "#value < 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "spel");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
    }

    // ────────────────────────────────────────────────
    // SPEL TEST — DEFAULT (no parser specified)
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_SpEL_Default() {

        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "#value == 5");
        routeMap.put("routeB", "#value >= 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        // no parser → default SpEL

        forkComponent.configure(config);

        context.set("value", 5);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
    }

    // ────────────────────────────────────────────────
    // KOTLIN TEST
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_Kotlin() {
        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "value > 10");
        routeMap.put("routeB", "value < 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "kotlin");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
    }

    // ────────────────────────────────────────────────
    // GROOVY TEST
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_Groovy() {
        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "value > 10");
        routeMap.put("routeB", "value < 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "groovy");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
    }

    // ────────────────────────────────────────────────
    // HELPER EMITTER
    // ────────────────────────────────────────────────
    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;

        @Override
        public void success(Context updatedContext) {
            successCalled = true;
        }

        @Override
        public void failure(Exception ex) {
            fail("Unexpected failure: " + ex.getMessage());
        }

        @Override
        public Logger logger() {
            return (msg, level, throwable) -> {
                /* ignore */ };
        }
    }

    // ────────────────────────────────────────────────
    // EXCLUSIVE MODE TEST
    // ────────────────────────────────────────────────
    @Test
    void testRun_EvaluatesRoutes_Exclusive() {
        // Use LinkedHashMap to ensure order
        Map<String, String> routeMap = new java.util.LinkedHashMap<>();
        routeMap.put("routeA", "#value > 10"); // true
        routeMap.put("routeB", "#value > 5"); // also true, but should be skipped/false in exclusive
        routeMap.put("routeC", "true"); // also true, but should be skipped/false

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_FORK_MODE, "EXCLUSIVE");
        config.set(ForkComponent.CONF_PARSER, "spel");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);

        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();

        assertEquals(true, options.get("routeA"), "First match should be true");
        assertEquals(false, options.get("routeB"), "Second match should be false in exclusive mode");
        assertEquals(false, options.get("routeC"), "Subsequent match should be false in exclusive mode");
    }
}
