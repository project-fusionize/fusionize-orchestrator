package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForkComponentAdditionalTest {

    private ForkComponent forkComponent;
    private Context context;
    private CapturingEmitter emitter;

    @BeforeEach
    void setUp() {
        // setup
        forkComponent = new ForkComponent();
        context = new Context();

        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowNodeKey("forkNode");
        context.setRuntimeData(runtimeData);

        emitter = new CapturingEmitter();
    }

    // ────────────────────────────────────────────────
    // canActivate
    // ────────────────────────────────────────────────

    @Test
    void shouldCallSuccessOnCanActivate() {
        // setup
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        forkComponent.configure(config);

        // expectation
        forkComponent.canActivate(context, emitter);

        // validation
        assertThat(emitter.successCalled).isTrue();
        assertThat(emitter.successContext).isSameAs(context);
    }

    // ────────────────────────────────────────────────
    // EXCLUSIVE MODE — matching condition with JS engine
    // ────────────────────────────────────────────────

    @Test
    void shouldRunExclusiveMode_withMatchingCondition_JS() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "value == 5");
        routeMap.put("routeB", "value > 3");
        routeMap.put("routeC", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_FORK_MODE, "EXCLUSIVE");
        config.set(ForkComponent.CONF_PARSER, "js");

        forkComponent.configure(config);

        context.set("value", 5);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("routeA")).isTrue();
        assertThat(options.get("routeB")).isFalse();
        assertThat(options.get("routeC")).isFalse();
    }

    // ────────────────────────────────────────────────
    // EXCLUSIVE MODE — no match, default path used
    // ────────────────────────────────────────────────

    @Test
    void shouldRunExclusiveMode_withDefaultPath() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "#value > 100");
        routeMap.put("routeB", "#value > 200");
        routeMap.put("defaultRoute", "#value > 300");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_FORK_MODE, "EXCLUSIVE");
        config.set(ForkComponent.CONF_DEFAULT_PATH, "defaultRoute");

        forkComponent.configure(config);

        context.set("value", 1);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("routeA")).isFalse();
        assertThat(options.get("routeB")).isFalse();
        assertThat(options.get("defaultRoute")).isTrue();
    }

    // ────────────────────────────────────────────────
    // INCLUSIVE MODE — all matching paths selected
    // ────────────────────────────────────────────────

    @Test
    void shouldRunInclusiveMode_allMatchingPathsSelected() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "#value > 3");
        routeMap.put("routeB", "#value > 2");
        routeMap.put("routeC", "#value < 0");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        // INCLUSIVE is the default, but set explicitly for clarity
        config.set(ForkComponent.CONF_FORK_MODE, "INCLUSIVE");

        forkComponent.configure(config);

        context.set("value", 5);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("routeA")).isTrue();
        assertThat(options.get("routeB")).isTrue();
        assertThat(options.get("routeC")).isFalse();
    }

    // ────────────────────────────────────────────────
    // NO CONDITIONS MATCH, NO DEFAULT — failure
    // ────────────────────────────────────────────────

    @Test
    void shouldFailWhenNoConditionMetAndNoDefaultPath() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "#value > 100");
        routeMap.put("routeB", "#value > 200");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);

        forkComponent.configure(config);

        context.set("value", 1);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        assertThat(emitter.successCalled).isFalse();
        assertThat(emitter.failureException).isNotNull();
        assertThat(emitter.failureException.getMessage())
                .contains("No condition met and no default path defined");
    }

    // ────────────────────────────────────────────────
    // DEFAULT PATH NOT IN CONDITION MAP — failure
    // ────────────────────────────────────────────────

    @Test
    void shouldFailWhenDefaultPathNotInConditionMap() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "#value > 100");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_DEFAULT_PATH, "nonExistentRoute");

        forkComponent.configure(config);

        context.set("value", 1);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        assertThat(emitter.successCalled).isFalse();
        assertThat(emitter.failureException).isNotNull();
        assertThat(emitter.failureException.getMessage())
                .contains("No condition met and no default path defined");
    }

    // ────────────────────────────────────────────────
    // CONDITION EXPRESSION THROWS EXCEPTION — logs error
    // ────────────────────────────────────────────────

    @Test
    void shouldLogErrorWhenConditionExpressionFails() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("badRoute", "#nonExistentVar.call()");
        routeMap.put("goodRoute", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);

        forkComponent.configure(config);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("badRoute")).isFalse();
        assertThat(options.get("goodRoute")).isTrue();
        assertThat(emitter.logMessages).anyMatch(msg -> msg.contains("failed"));
    }

    // ────────────────────────────────────────────────
    // CONFIGURE WITH KTS PARSER
    // ────────────────────────────────────────────────

    @Test
    void shouldConfigureWithKtsParser() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "value > 10");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "kts");

        forkComponent.configure(config);

        context.set("value", 15);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("routeA")).isTrue();
    }

    // ────────────────────────────────────────────────
    // CONFIGURE WITH EMPTY CONDITIONS MAP
    // ────────────────────────────────────────────────

    @Test
    void shouldHandleEmptyConditionsMap() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);

        forkComponent.configure(config);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        assertThat(emitter.successCalled).isFalse();
        assertThat(emitter.failureException).isNotNull();
    }

    // ────────────────────────────────────────────────
    // CONFIGURE WITH NO CONDITIONS KEY
    // ────────────────────────────────────────────────

    @Test
    void shouldHandleMissingConditionsConfig() {
        // setup
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();

        forkComponent.configure(config);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        assertThat(emitter.successCalled).isFalse();
        assertThat(emitter.failureException).isNotNull();
    }

    // ────────────────────────────────────────────────
    // STATIC FIELD VALUES
    // ────────────────────────────────────────────────

    @Test
    void shouldHaveCorrectStaticFieldValues() {
        // setup
        // no setup needed

        // expectation
        // no action needed, just validate constants

        // validation
        assertThat(ForkComponent.NAME).isEqualTo("fork");
        assertThat(ForkComponent.CONF_FORK_MODE).isEqualTo("forkMode");
        assertThat(ForkComponent.CONF_CONDITIONS).isEqualTo("conditions");
        assertThat(ForkComponent.CONF_DEFAULT_PATH).isEqualTo("default");
        assertThat(ForkComponent.CONF_PARSER).isEqualTo("parser");
    }

    // ────────────────────────────────────────────────
    // FORK MODE ENUM VALUES
    // ────────────────────────────────────────────────

    @Test
    void shouldHaveCorrectForkModeEnumValues() {
        // setup
        // no setup needed

        // expectation
        var values = ForkComponent.ForkMode.values();

        // validation
        assertThat(values).hasSize(2);
        assertThat(ForkComponent.ForkMode.valueOf("INCLUSIVE")).isEqualTo(ForkComponent.ForkMode.INCLUSIVE);
        assertThat(ForkComponent.ForkMode.valueOf("EXCLUSIVE")).isEqualTo(ForkComponent.ForkMode.EXCLUSIVE);
    }

    // ────────────────────────────────────────────────
    // EXCLUSIVE MODE WITH GROOVY ENGINE
    // ────────────────────────────────────────────────

    @Test
    void shouldRunExclusiveMode_withMatchingCondition_Groovy() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "value == 5");
        routeMap.put("routeB", "value > 3");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_FORK_MODE, "EXCLUSIVE");
        config.set(ForkComponent.CONF_PARSER, "groovy");

        forkComponent.configure(config);

        context.set("value", 5);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("routeA")).isTrue();
        assertThat(options.get("routeB")).isFalse();
    }

    // ────────────────────────────────────────────────
    // SCRIPT ENGINE CONDITION THROWS EXCEPTION
    // ────────────────────────────────────────────────

    @Test
    void shouldLogErrorWhenScriptEngineConditionFails() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("badRoute", "undefinedVar.nonExistentMethod()");
        routeMap.put("goodRoute", "true");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_PARSER, "js");

        forkComponent.configure(config);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("badRoute")).isFalse();
        assertThat(options.get("goodRoute")).isTrue();
        assertThat(emitter.logMessages).anyMatch(msg -> msg.contains("failed"));
    }

    // ────────────────────────────────────────────────
    // INCLUSIVE DEFAULT PATH FALLBACK
    // ────────────────────────────────────────────────

    @Test
    void shouldRunInclusiveMode_defaultPathFallback() {
        // setup
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("routeA", "#value > 100");
        routeMap.put("fallback", "#value > 200");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ForkComponent.CONF_CONDITIONS, routeMap);
        config.set(ForkComponent.CONF_DEFAULT_PATH, "fallback");

        forkComponent.configure(config);

        context.set("value", 1);

        WorkflowDecision decision = new WorkflowDecision();
        decision.setDecisionNode("forkNode");
        context.getDecisions().add(decision);

        // expectation
        forkComponent.run(context, emitter);

        // validation
        Map<String, Boolean> options = context.getDecisions().getFirst().getOptionNodes();
        assertThat(emitter.successCalled).isTrue();
        assertThat(options.get("fallback")).isTrue();
    }

    // ────────────────────────────────────────────────
    // HELPER EMITTER
    // ────────────────────────────────────────────────

    static class CapturingEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        Context successContext = null;
        Exception failureException = null;
        List<String> logMessages = new ArrayList<>();

        @Override
        public void success(Context updatedContext) {
            successCalled = true;
            successContext = updatedContext;
        }

        @Override
        public void failure(Exception ex) {
            failureException = ex;
        }

        @Override
        public Logger logger() {
            return (msg, level, throwable) -> {
                if (msg != null) {
                    logMessages.add(msg);
                }
            };
        }

        @Override
        public InteractionLogger interactionLogger() {
            return null;
        }
    }
}
