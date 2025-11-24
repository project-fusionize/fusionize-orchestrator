package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.script.engine.ScriptRunner;
import dev.fusionize.script.engine.ScriptRunnerEngine;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * ForkComponent evaluates a map of conditions and routes execution to matching
 * paths.
 * <p>
 * Configuration:
 * <ul>
 * <li>{@code routeMap}: Map of node names to boolean expressions
 * (scripts).</li>
 * </ul>
 */
public class ForkComponent implements LocalComponentRuntime {

    public static final String CONF_CONDITIONS = "conditions";
    private final Map<String, String> routeMap = new HashMap<>();
    private final ScriptRunner scriptRunner;

    public ForkComponent() {
        this.scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varMap(CONF_CONDITIONS).ifPresent(map -> {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (key instanceof String && value instanceof String) {
                    routeMap.put((String) key, (String) value);
                }
            }
        });
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        // ForkComponent is a decision node, it activates immediately when reached.
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        WorkflowDecision decision = context.getDecisionToRun();
        Map<String, Boolean> optionNodes = new HashMap<>();
        Map<String, Object> scriptContext = new HashMap<>(context.getData());

        for (Map.Entry<String, String> entry : routeMap.entrySet()) {
            String nodeName = entry.getKey();
            String script = entry.getValue();
            try {
                Object result = scriptRunner.eval(script, scriptContext);
                boolean isTrue = Boolean.TRUE.equals(result);
                optionNodes.put(nodeName, isTrue);
            } catch (ScriptException | NoSuchMethodException e) {
                emitter.logger().error("Error evaluating script for route '{}': {}", nodeName, e.getMessage());
                optionNodes.put(nodeName, false);
            }
        }

        decision.setOptionNodes(optionNodes);
        context.getDecisions().add(decision);
        emitter.success(context);
    }
}
