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
import java.util.Map;

/**
 * ForkComponent evaluates a set of conditions and chooses outgoing paths.
 */
public class ForkComponent implements LocalComponentRuntime {

    public static final String CONF_PARSER = "parser";
    public static final String CONF_CONDITIONS = "conditions";
    public static final String CONF_DEFAULT_PATH = "default";

    private final Map<String, String> conditionMap = new HashMap<>();
    private String defaultPath = null;

    private ScriptRunnerEngine engine = null;            // JS, Groovy, Kotlin
    private ScriptRunner scriptRunner = null;

    // Default to SpEL unless engine is explicitly configured
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private boolean useSpEL = true;


    // ────────────────────────────────────────────────
    // CONFIGURE()
    // ────────────────────────────────────────────────
    @Override
    public void configure(ComponentRuntimeConfig config) {

        // Load conditions map
        config.varMap(CONF_CONDITIONS).ifPresent(map -> {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (key instanceof String && value instanceof String) {
                    conditionMap.put((String) key, (String) value);
                }
            }
        });

        // Load default path
        config.varString(CONF_DEFAULT_PATH).ifPresent(s -> this.defaultPath = s);

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

        for (Map.Entry<String, String> entry : conditionMap.entrySet()) {
            String node = entry.getKey();
            String expr = entry.getValue();

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
            }
            catch (Exception e) {
                emitter.logger().error("Condition '{}' failed: {}", node, e.getMessage());
            }

            optionNodes.put(node, result);
        }

        // Store decision result
        WorkflowDecision decision = context.getDecisionToRun();
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
