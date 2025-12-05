package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.common.utility.TextUtil;
import dev.fusionize.script.engine.ScriptHelper;
import dev.fusionize.script.engine.ScriptRunner;
import dev.fusionize.script.engine.ScriptRunnerEngine;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptComponent implements LocalComponentRuntime {
    public static final String NAME = "script";

    public static final String CONF_PARSER = "parser";
    public static final String CONF_SCRIPT = "script";

    private ScriptRunnerEngine engine = ScriptRunnerEngine.JS;
    private ScriptRunner scriptRunner = null;
    private String script;

    // ────────────────────────────────────────────────
    // CONFIGURE()
    // ────────────────────────────────────────────────
    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_SCRIPT).ifPresent(s -> script = s);
        config.varString(CONF_PARSER).ifPresent(s -> {
            if (TextUtil.matchesFlexible("js", s)) {
                engine = ScriptRunnerEngine.JS;
            } else if (TextUtil.matchesFlexible("kotlin", s)
                    || TextUtil.matchesFlexible("kts", s)) {
                engine = ScriptRunnerEngine.KOTLIN;
            } else if (TextUtil.matchesFlexible("groovy", s)) {
                engine = ScriptRunnerEngine.GROOVY;
            }
        });

        scriptRunner = new ScriptRunner(engine);
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
        try {
            Context cloneContext = context.renew();
            ConcurrentHashMap<String, Object> ctxData = new ConcurrentHashMap<>(cloneContext.getData());

            Object nativeCtx = new ScriptHelper().createNativeContextObject(ctxData, engine);

            Map<String, Object> evalContext = new HashMap<>(cloneContext.getData());
            evalContext.put("context", nativeCtx);

            // Evaluate script; returning 'context' ensures scripts can modify and return it
            if(ScriptRunnerEngine.JS.equals(engine)){
                script = script + "; context;";
            }

            Object result = scriptRunner.eval(script, evalContext);

            // If script returns a Map, merge it back
            if (result instanceof Map) {
                Map<?, ?> resultMap = (Map<?, ?>) result;
                resultMap.forEach((k, v) -> {
                    if (k instanceof String) {
                        ctxData.put((String) k, v);
                    }
                });
            }

            emitter.logger().info("Script ran successfully {}", ctxData);

            cloneContext.setData(ctxData);
            emitter.success(cloneContext);

        } catch (Exception e) {
            emitter.logger().error("Script failed: {}", e.getMessage());
            emitter.failure(e);
        }
    }


}
