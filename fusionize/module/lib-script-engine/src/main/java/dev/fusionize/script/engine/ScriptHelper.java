package dev.fusionize.script.engine;

import org.graalvm.polyglot.Value;

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
            return new JsContextWrapper(ctxData);
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
            Object val = data.get(name);
            if (val instanceof Map) {
                return new ContextGroovyObject((Map<String, Object>) val);
            }
            return val;
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
            Object val = data.get(key);
            if (val instanceof Map) {
                return new ContextWrapper((Map<String, Object>) val);
            }
            return val;
        }

        public void set(String key, Object value) {
            data.put(key, value);
        }

        public Map<String,Object> asMap() {
            return data;
        }
    }

    public static class JsContextWrapper implements org.graalvm.polyglot.proxy.ProxyObject {
        private final Map<String, Object> data;

        public JsContextWrapper(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Object getMember(String key) {
            Object val = data.get(key);
            if (val instanceof Map) {
                return new JsContextWrapper((Map<String, Object>) val);
            }
            return val;
        }

        @Override
        public Object getMemberKeys() {
            return org.graalvm.polyglot.proxy.ProxyArray.fromList(new java.util.ArrayList<>(data.keySet()));
        }

        @Override
        public boolean hasMember(String key) {
            return data.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            data.put(key, value);
        }

        @Override
        public boolean removeMember(String key) {
            if (data.containsKey(key)) {
                data.remove(key);
                return true;
            }
            return false;
        }
    }
}
