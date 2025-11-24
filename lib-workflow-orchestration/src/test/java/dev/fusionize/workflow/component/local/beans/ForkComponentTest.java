package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkComponentTest {

    private ForkComponent forkComponent;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        forkComponent = new ForkComponent();
        context = new Context();
        dev.fusionize.workflow.context.ContextRuntimeData runtimeData = new dev.fusionize.workflow.context.ContextRuntimeData();
        runtimeData.setWorkflowNodeId("forkNode");
        context.setRuntimeData(runtimeData);
        emitter = new TestEmitter();
    }

    @Test
    void testRun_EvaluatesRoutes() {
        // Configure routes
        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("routeA", "value > 10");
        routeMap.put("routeB", "value < 10");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        forkComponent.configure(config);

        // Set context data
        context.set("value", 15);

        // Run
        forkComponent.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertEquals(1, context.getDecisions().size());
        WorkflowDecision decision = context.getDecisions().get(0);
        assertEquals("forkNode", decision.getDecisionNode());

        Map<String, Boolean> options = decision.getOptionNodes();
        assertEquals(true, options.get("routeA"));
        assertEquals(false, options.get("routeB"));
        assertEquals(true, options.get("routeC"));
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
