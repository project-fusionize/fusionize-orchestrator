package dev.fusionize.script.engine;

import java.util.Map;

public class ScriptHelper {
    public Object createNativeContextObject(Map<String, Object> ctxData, ScriptRunnerEngine engine) {


        // Kotlin Script
        if (ScriptRunnerEngine.KOTLIN.equals(engine)) {
            return new ContextWrapper(ctxData);
        }

        // Groovy Script
        if (ScriptRunnerEngine.GROOVY.equals(engine)) {
            return new ContextGroovyObject(ctxData);
        }

        // JavaScript (GraalJS)
        if (ScriptRunnerEngine.JS.equals(engine))  {
            return org.graalvm.polyglot.proxy.ProxyObject.fromMap(ctxData);
        }

        // Fallback: plain map
        return ctxData;
    }

    public static class ContextGroovyObject extends groovy.lang.GroovyObjectSupport {

        private final Map<String, Object> data;

        public ContextGroovyObject(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Object getProperty(String name) {
            return data.get(name);
        }

        @Override
        public void setProperty(String name, Object value) {
            data.put(name, value);
        }
    }

    public static class ContextWrapper {
        private final Map<String, Object> data;

        public ContextWrapper(Map<String, Object> data) {
            this.data = data;
        }

        public Object get(String key) {
            return data.get(key);
        }

        public void set(String key, Object value) {
            data.put(key, value);
        }

        public Map<String,Object> asMap() {
            return data;
        }
    }
}
