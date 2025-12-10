package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRuntimeConfigTest {

    @Test
    void builder_ShouldCreateConfigWithValues() {
        ComponentRuntimeConfig config = ComponentRuntimeConfig.builder()
                .put("key1", "value1")
                .put("key2", 123)
                .build();

        assertNotNull(config);
        assertEquals("value1", config.getConfig().get("key1"));
        assertEquals(123, config.getConfig().get("key2"));
    }

    @Test
    void builder_ShouldPutAll() {
        Map<String, Object> map = Map.of("k1", "v1", "k2", "v2");
        ComponentRuntimeConfig config = ComponentRuntimeConfig.builder()
                .putAll(map)
                .build();

        assertEquals(2, config.getConfig().size());
        assertEquals("v1", config.getConfig().get("k1"));
        assertEquals("v2", config.getConfig().get("k2"));
    }

    @Test
    void from_ShouldCreateFromComponentConfig() {
        ComponentConfig componentConfig = new ComponentConfig();
        componentConfig.setConfig(new ConcurrentHashMap<>(Map.of("key", "value")));

        ComponentRuntimeConfig runtimeConfig = ComponentRuntimeConfig.from(componentConfig);

        assertNotNull(runtimeConfig);
        assertEquals("value", runtimeConfig.getConfig().get("key"));
    }

    @Test
    void from_WithNull_ShouldReturnEmptyConfig() {
        ComponentRuntimeConfig runtimeConfig = ComponentRuntimeConfig.from(null);
        assertNotNull(runtimeConfig);
        assertTrue(runtimeConfig.getConfig().isEmpty());
    }

    @Test
    void var_ShouldReturnTypedOptional() {
        ComponentRuntimeConfig config = ComponentRuntimeConfig.builder()
                .put("string", "hello")
                .put("int", 123)
                .put("double", 12.34)
                .put("list", List.of("a", "b"))
                .build();

        // String
        Optional<String> s = config.varString("string");
        assertTrue(s.isPresent());
        assertEquals("hello", s.get());

        // Integer
        Optional<Integer> i = config.varInt("int");
        assertTrue(i.isPresent());
        assertEquals(123, i.get());

        // Double
        Optional<Double> d = config.varDouble("double");
        assertTrue(d.isPresent());
        assertEquals(12.34, d.get());

        // List
        Optional<List> l = config.varList("list");
        assertTrue(l.isPresent());
        assertEquals(2, l.get().size());

        // Missing key
        assertTrue(config.varString("missing").isEmpty());

        // Wrong type
        assertTrue(config.varInt("string").isEmpty());
    }

    @Test
    void contains_ShouldReturnTrueIfKeyExists() {
        ComponentRuntimeConfig config = ComponentRuntimeConfig.builder()
                .put("key", "value")
                .build();

        assertTrue(config.contains("key"));
        assertFalse(config.contains("missing"));
    }

    @Test
    void set_ShouldUpdateValue() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("key", "value1");
        assertEquals("value1", config.getConfig().get("key"));

        config.set("key", "value2");
        assertEquals("value2", config.getConfig().get("key"));
    }
}
