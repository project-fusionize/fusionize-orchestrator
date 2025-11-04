package dev.fusionize.script.engine;

import java.util.HashMap;
import java.util.Map;

public enum ScriptRunnerEngine {
    JS("graal.js"),
    KOTLIN("kotlin"),
    GROOVY("groovy");

    private final String name;

    ScriptRunnerEngine(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, ScriptRunnerEngine> lookup = new HashMap<>();

    static{
        for(ScriptRunnerEngine engine : ScriptRunnerEngine.values())
            lookup.put(engine.getName(), engine);
    }
    public static ScriptRunnerEngine get(String name){
        return lookup.get(name);
    }
}
