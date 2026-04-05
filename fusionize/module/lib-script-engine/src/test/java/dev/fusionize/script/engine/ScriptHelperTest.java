package dev.fusionize.script.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptHelperTest {

    private final ScriptHelper scriptHelper = new ScriptHelper();

    @Test
    void shouldReturnContextWrapper_forKotlin() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        // expectation
        var result = scriptHelper.createNativeContextObject(data, ScriptRunnerEngine.KOTLIN);

        // validation
        assertThat(result).isInstanceOf(ScriptHelper.ContextWrapper.class);
    }

    @Test
    void shouldReturnContextGroovyObject_forGroovy() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        // expectation
        var result = scriptHelper.createNativeContextObject(data, ScriptRunnerEngine.GROOVY);

        // validation
        assertThat(result).isInstanceOf(ScriptHelper.ContextGroovyObject.class);
    }

    @Test
    void shouldReturnJsContextWrapper_forJs() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        // expectation
        var result = scriptHelper.createNativeContextObject(data, ScriptRunnerEngine.JS);

        // validation
        assertThat(result).isInstanceOf(ScriptHelper.JsContextWrapper.class);
    }

    @Test
    void shouldReturnRawMap_forNull() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        // expectation
        var result = scriptHelper.createNativeContextObject(data, null);

        // validation
        assertThat(result).isSameAs(data);
    }

    @Test
    void shouldContextWrapperGetAndSet() {
        // setup
        Map<String, Object> data = new HashMap<>();
        var wrapper = new ScriptHelper.ContextWrapper(data);

        // expectation
        wrapper.set("key", "val");
        var result = wrapper.get("key");

        // validation
        assertThat(result).isEqualTo("val");
    }

    @Test
    void shouldContextWrapperAsMap() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        var wrapper = new ScriptHelper.ContextWrapper(data);

        // expectation
        var result = wrapper.asMap();

        // validation
        assertThat(result).isSameAs(data);
    }

    @Test
    void shouldGroovyObjectGetAndSetProperty() {
        // setup
        Map<String, Object> data = new HashMap<>();
        var groovyObject = new ScriptHelper.ContextGroovyObject(data);

        // expectation
        groovyObject.setProperty("name", "test");
        var result = groovyObject.getProperty("name");

        // validation
        assertThat(result).isEqualTo("test");
    }

    @Test
    void shouldJsContextWrapperHasMember() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("existing", "value");
        var jsWrapper = new ScriptHelper.JsContextWrapper(data);

        // expectation
        var hasExisting = jsWrapper.hasMember("existing");
        var hasMissing = jsWrapper.hasMember("missing");

        // validation
        assertThat(hasExisting).isTrue();
        assertThat(hasMissing).isFalse();
    }

    @Test
    void shouldJsContextWrapperRemoveMember() {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put("toRemove", "value");
        var jsWrapper = new ScriptHelper.JsContextWrapper(data);

        // expectation
        var removed = jsWrapper.removeMember("toRemove");

        // validation
        assertThat(removed).isTrue();
        assertThat(jsWrapper.hasMember("toRemove")).isFalse();
    }
}
