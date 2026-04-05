package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentRuntimeConfigAdditionalTest {

    @Test
    void shouldCreateFromComponentConfig() {
        // setup
        var componentConfig = new ComponentConfig();
        componentConfig.setConfig(new ConcurrentHashMap<>(Map.of("key", "value", "num", 42)));

        // expectation
        var runtimeConfig = ComponentRuntimeConfig.from(componentConfig);

        // validation
        assertThat(runtimeConfig).isNotNull();
        assertThat(runtimeConfig.getConfig()).containsEntry("key", "value");
        assertThat(runtimeConfig.getConfig()).containsEntry("num", 42);
    }

    @Test
    void shouldCreateFromNullComponentConfig() {
        // setup
        ComponentConfig nullConfig = null;

        // expectation
        var runtimeConfig = ComponentRuntimeConfig.from(nullConfig);

        // validation
        assertThat(runtimeConfig).isNotNull();
        assertThat(runtimeConfig.getConfig()).isEmpty();
    }

    @Test
    void shouldBuildWithPut() {
        // setup
        var builder = ComponentRuntimeConfig.builder();

        // expectation
        var config = builder.put("alpha", "beta").build();

        // validation
        assertThat(config.getConfig()).containsEntry("alpha", "beta");
        assertThat(config.getConfig()).hasSize(1);
    }

    @Test
    void shouldBuildWithPutAll() {
        // setup
        var values = Map.<String, Object>of("a", 1, "b", 2, "c", 3);

        // expectation
        var config = ComponentRuntimeConfig.builder().putAll(values).build();

        // validation
        assertThat(config.getConfig()).hasSize(3);
        assertThat(config.getConfig()).containsEntry("a", 1);
        assertThat(config.getConfig()).containsEntry("b", 2);
        assertThat(config.getConfig()).containsEntry("c", 3);
    }

    @Test
    void shouldBuildWithPutIfAbsent() {
        // setup
        var builder = ComponentRuntimeConfig.builder()
                .put("existing", "original");

        // expectation
        var config = builder
                .putIfAbsent("existing", "overwritten")
                .putIfAbsent("newKey", "newValue")
                .build();

        // validation
        assertThat(config.getConfig()).containsEntry("existing", "original");
        assertThat(config.getConfig()).containsEntry("newKey", "newValue");
    }

    @Test
    void shouldBuildWithRemove() {
        // setup
        var builder = ComponentRuntimeConfig.builder()
                .put("keep", "yes")
                .put("remove", "no");

        // expectation
        var config = builder.remove("remove").build();

        // validation
        assertThat(config.getConfig()).containsEntry("keep", "yes");
        assertThat(config.getConfig()).doesNotContainKey("remove");
    }

    @Test
    void shouldBuildWithClear() {
        // setup
        var builder = ComponentRuntimeConfig.builder()
                .put("a", 1)
                .put("b", 2);

        // expectation
        var config = builder.clear().build();

        // validation
        assertThat(config.getConfig()).isEmpty();
    }

    @Test
    void shouldBuildWithMapConfig() {
        // setup
        var map = Map.<String, Object>of("x", "y", "z", 99);

        // expectation
        var config = ComponentRuntimeConfig.builder().withConfig(map).build();

        // validation
        assertThat(config.getConfig()).hasSize(2);
        assertThat(config.getConfig()).containsEntry("x", "y");
        assertThat(config.getConfig()).containsEntry("z", 99);
    }

    @Test
    void shouldBuildWithNullMapConfig() {
        // setup
        Map<String, Object> nullMap = null;

        // expectation
        var config = ComponentRuntimeConfig.builder().withConfig(nullMap).build();

        // validation
        assertThat(config.getConfig()).isEmpty();
    }

    @Test
    void shouldReturnVarBoolean() {
        // setup
        var config = ComponentRuntimeConfig.builder()
                .put("flag", true)
                .build();

        // expectation
        var result = config.varBoolean("flag");

        // validation
        assertThat(result).isPresent();
        assertThat(result.get()).isTrue();
    }

    @Test
    void shouldReturnEmpty_whenTypeMismatch() {
        // setup
        var config = ComponentRuntimeConfig.builder()
                .put("value", "not-a-number")
                .build();

        // expectation
        var intResult = config.varInt("value");
        var doubleResult = config.varDouble("value");
        var floatResult = config.varFloat("value");
        var boolResult = config.varBoolean("value");

        // validation
        assertThat(intResult).isEmpty();
        assertThat(doubleResult).isEmpty();
        assertThat(floatResult).isEmpty();
        assertThat(boolResult).isEmpty();
    }

    @Test
    void shouldBeEqual_whenSameConfig() {
        // setup
        var config1 = ComponentRuntimeConfig.builder()
                .put("key", "value")
                .build();
        var config2 = ComponentRuntimeConfig.builder()
                .put("key", "value")
                .build();

        // expectation
        var areEqual = config1.equals(config2);
        var sameHash = config1.hashCode() == config2.hashCode();

        // validation
        assertThat(areEqual).isTrue();
        assertThat(sameHash).isTrue();
    }

    @Test
    void shouldNotBeEqual_whenDifferentConfig() {
        // setup
        var config1 = ComponentRuntimeConfig.builder()
                .put("key", "value1")
                .build();
        var config2 = ComponentRuntimeConfig.builder()
                .put("key", "value2")
                .build();

        // expectation
        var areEqual = config1.equals(config2);

        // validation
        assertThat(areEqual).isFalse();
    }
}
