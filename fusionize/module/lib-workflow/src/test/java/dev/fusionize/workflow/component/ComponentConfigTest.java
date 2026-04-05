package dev.fusionize.workflow.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentConfigTest {

    @Test
    void shouldCreateEmptyConfig() {
        // setup
        var config = new ComponentConfig();

        // expectation
        var result = config.getConfig();

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldBuildWithPut() {
        // setup
        var config = ComponentConfig.builder()
                .put("key", "value")
                .build();

        // expectation
        var containsKey = config.contains("key");

        // validation
        assertThat(containsKey).isTrue();
        assertThat(config.varString("key")).hasValue("value");
    }

    @Test
    void shouldReturnVarString() {
        // setup
        var config = new ComponentConfig();
        config.set("name", "fusionize");

        // expectation
        var result = config.varString("name");

        // validation
        assertThat(result).isPresent().hasValue("fusionize");
    }

    @Test
    void shouldReturnVarInt() {
        // setup
        var config = new ComponentConfig();
        config.set("count", 42);

        // expectation
        var result = config.varInt("count");

        // validation
        assertThat(result).isPresent().hasValue(42);
    }

    @Test
    void shouldReturnVarDouble() {
        // setup
        var config = new ComponentConfig();
        config.set("rate", 3.14);

        // expectation
        var result = config.varDouble("rate");

        // validation
        assertThat(result).isPresent().hasValue(3.14);
    }

    @Test
    void shouldReturnVarFloat() {
        // setup
        var config = new ComponentConfig();
        config.set("ratio", 1.5f);

        // expectation
        var result = config.varFloat("ratio");

        // validation
        assertThat(result).isPresent().hasValue(1.5f);
    }

    @Test
    void shouldReturnVarList() {
        // setup
        var config = new ComponentConfig();
        config.set("items", List.of("a", "b", "c"));

        // expectation
        var result = config.varList("items");

        // validation
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("a", "b", "c");
    }

    @Test
    void shouldReturnVarMap() {
        // setup
        var config = new ComponentConfig();
        config.set("metadata", Map.of("k1", "v1"));

        // expectation
        var result = config.varMap("metadata");

        // validation
        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("k1", "v1");
    }

    @Test
    void shouldReturnEmpty_whenTypeMismatch() {
        // setup
        var config = new ComponentConfig();
        config.set("number", 123);

        // expectation
        var result = config.varString("number");

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenKeyNotFound() {
        // setup
        var config = new ComponentConfig();

        // expectation
        var result = config.varString("nonexistent");

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldContainKey_whenSet() {
        // setup
        var config = new ComponentConfig();
        config.set("present", "yes");

        // expectation
        var result = config.contains("present");

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotContainKey_whenNotSet() {
        // setup
        var config = new ComponentConfig();

        // expectation
        var result = config.contains("absent");

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldSetValue() {
        // setup
        var config = new ComponentConfig();

        // expectation
        config.set("key", "val");

        // validation
        assertThat(config.contains("key")).isTrue();
        assertThat(config.varString("key")).hasValue("val");
    }

    @Test
    void shouldBuildWithPutAll() {
        // setup
        var values = Map.<String, Object>of("a", 1, "b", 2, "c", 3);

        // expectation
        var config = ComponentConfig.builder()
                .putAll(values)
                .build();

        // validation
        assertThat(config.getConfig()).hasSize(3);
        assertThat(config.varInt("a")).hasValue(1);
        assertThat(config.varInt("b")).hasValue(2);
        assertThat(config.varInt("c")).hasValue(3);
    }

    @Test
    void shouldBuildWithPutIfAbsent() {
        // setup
        var builder = ComponentConfig.builder()
                .put("key", "original");

        // expectation
        var config = builder
                .putIfAbsent("key", "replacement")
                .build();

        // validation
        assertThat(config.varString("key")).hasValue("original");
    }

    @Test
    void shouldBuildWithRemove() {
        // setup
        var builder = ComponentConfig.builder()
                .put("toRemove", "value");

        // expectation
        var config = builder
                .remove("toRemove")
                .build();

        // validation
        assertThat(config.contains("toRemove")).isFalse();
        assertThat(config.getConfig()).isEmpty();
    }

    @Test
    void shouldBuildWithClear() {
        // setup
        var builder = ComponentConfig.builder()
                .put("a", 1)
                .put("b", 2);

        // expectation
        var config = builder
                .clear()
                .build();

        // validation
        assertThat(config.getConfig()).isEmpty();
    }

    @Test
    void shouldBuildWithMapConfig() {
        // setup
        var map = Map.<String, Object>of("x", "hello", "y", 99);

        // expectation
        var config = ComponentConfig.builder()
                .withConfig(map)
                .build();

        // validation
        assertThat(config.varString("x")).hasValue("hello");
        assertThat(config.varInt("y")).hasValue(99);
    }

    @Test
    void shouldHandleNullConfig_inWithConfig() {
        // setup
        Map<String, Object> nullMap = null;

        // expectation
        var config = ComponentConfig.builder()
                .withConfig(nullMap)
                .build();

        // validation
        assertThat(config.getConfig()).isEmpty();
    }

    @Test
    void shouldBeEqual_whenSameConfig() {
        // setup
        var config1 = ComponentConfig.builder().put("key", "value").build();
        var config2 = ComponentConfig.builder().put("key", "value").build();

        // expectation
        var result = config1.equals(config2);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotBeEqual_whenDifferentConfig() {
        // setup
        var config1 = ComponentConfig.builder().put("key", "value1").build();
        var config2 = ComponentConfig.builder().put("key", "value2").build();

        // expectation
        var result = config1.equals(config2);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldHaveConsistentHashCode() {
        // setup
        var config1 = ComponentConfig.builder().put("key", "value").build();
        var config2 = ComponentConfig.builder().put("key", "value").build();

        // expectation
        var hash1 = config1.hashCode();
        var hash2 = config2.hashCode();

        // validation
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldIgnoreNullKey_inPut() {
        // setup
        var config = ComponentConfig.builder()
                .put(null, "value")
                .build();

        // expectation
        var result = config.getConfig();

        // validation
        assertThat(result).isEmpty();
    }
}
