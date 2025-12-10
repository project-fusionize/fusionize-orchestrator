package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.common.utility.TextUtil;
import dev.fusionize.script.engine.ScriptRunner;
import dev.fusionize.script.engine.ScriptRunnerEngine;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ForkComponent evaluates a set of conditions and chooses outgoing paths.
 */
public class ForkComponent implements LocalComponentRuntime {
    public static final String NAME = "fork";

    public static final String CONF_PARSER = "parser";
    public static final String CONF_CONDITIONS = "conditions";
    public static final String CONF_FORK_MODE = "forkMode";
    public static final String CONF_DEFAULT_PATH = "default";

    private final Map<String, String> conditionMap = new LinkedHashMap<>();
    private String defaultPath = null;
    private ForkMode forkMode = ForkMode.INCLUSIVE;

    private ScriptRunnerEngine engine = null; // JS, Groovy, Kotlin
    private ScriptRunner scriptRunner = null;

    // Default to SpEL unless engine is explicitly configured
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private boolean useSpEL = true;

    public enum ForkMode {
        INCLUSIVE,
        EXCLUSIVE,
    }

    // ────────────────────────────────────────────────
    // CONFIGURE()
    // ────────────────────────────────────────────────
    @Override
    public void configure(ComponentRuntimeConfig config) {

        // Load conditions map
        config.varMap(CONF_CONDITIONS).ifPresent(map -> {
            // Use LinkedHashMap to preserve order if the source map is ordered
            // (e.g. from YAML parsing that respects order)
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (key instanceof String && value instanceof String) {
                    conditionMap.put((String) key, (String) value);
                }
            }
        });

        // Load default path
        config.varString(CONF_DEFAULT_PATH).ifPresent(s -> this.defaultPath = s);

        // Load Fork Mode
        config.varString(CONF_FORK_MODE).ifPresent(s -> {
            if (TextUtil.matchesFlexible("EXCLUSIVE", s)) {
                this.forkMode = ForkMode.EXCLUSIVE;
            } else {
                this.forkMode = ForkMode.INCLUSIVE;
            }
        });

        // Detect expression parser OR script engine
        config.varString(CONF_PARSER).ifPresent(s -> {
            if (TextUtil.matchesFlexible("spel", s)) {
                useSpEL = true;
            } else if (TextUtil.matchesFlexible("js", s)) {
                engine = ScriptRunnerEngine.JS;
                useSpEL = false;
            } else if (TextUtil.matchesFlexible("kotlin", s)
                    || TextUtil.matchesFlexible("kts", s)) {
                engine = ScriptRunnerEngine.KOTLIN;
                useSpEL = false;
            } else if (TextUtil.matchesFlexible("groovy", s)) {
                engine = ScriptRunnerEngine.GROOVY;
                useSpEL = false;
            }
        });

        // If engine was selected → create ScriptRunner
        if (engine != null && !useSpEL) {
            scriptRunner = new ScriptRunner(engine);
        }
    }

    // ────────────────────────────────────────────────
    // CAN ACTIVATE
    // ────────────────────────────────────────────────
    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.success(context);
    }

    // ────────────────────────────────────────────────
    // RUN()
    // ────────────────────────────────────────────────
    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {

        Map<String, Boolean> optionNodes = new HashMap<>();
        Map<String, Object> evalContext = new HashMap<>(context.getData());

        boolean exclusiveMatchFound = false;

        for (Map.Entry<String, String> entry : conditionMap.entrySet()) {
            String node = entry.getKey();
            String expr = entry.getValue();

            // In EXCLUSIVE mode, if we already found a match, all subsequent are false
            if (forkMode == ForkMode.EXCLUSIVE && exclusiveMatchFound) {
                optionNodes.put(node, false);
                continue;
            }

            boolean result = false;

            try {
                if (useSpEL) {
                    // Evaluate with SpEL
                    StandardEvaluationContext spelCtx = new StandardEvaluationContext();
                    spelCtx.setVariables(evalContext);
                    result = Boolean.TRUE.equals(spelParser.parseExpression(expr).getValue(spelCtx, Boolean.class));

                } else if (scriptRunner != null) {
                    // Evaluate with ScriptRunner
                    Object val = scriptRunner.eval(expr, evalContext);
                    result = Boolean.TRUE.equals(val);
                }
            } catch (Exception e) {
                emitter.logger().error("Condition '{}' failed: {}", node, e.getMessage());
            }

            optionNodes.put(node, result);

            if (result && forkMode == ForkMode.EXCLUSIVE) {
                exclusiveMatchFound = true;
            }
        }

        // Store decision result
        WorkflowDecision decision = context.decisionToRun();
        decision.setOptionNodes(optionNodes);

        // Success cases
        if (optionNodes.values().stream().anyMatch(Boolean::booleanValue)) {
            emitter.logger().info("Evaluation results: {}", optionNodes);
            emitter.success(context);
            return;
        }

        // Default fallback
        if (defaultPath != null && optionNodes.containsKey(defaultPath)) {
            optionNodes.put(defaultPath, Boolean.TRUE);
            emitter.logger().info("Fork fallback to default: {}", defaultPath);
            emitter.success(context);
            return;
        }

        emitter.failure(new Exception("No condition met and no default path defined"));
    }
}
