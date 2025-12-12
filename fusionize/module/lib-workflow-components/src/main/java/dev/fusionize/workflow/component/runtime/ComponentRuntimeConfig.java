package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.ComponentConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentRuntimeConfig {
    private ConcurrentHashMap<String, Object> config = new ConcurrentHashMap<>();

    public ComponentRuntimeConfig() {
    }

    private ComponentRuntimeConfig(Builder builder) {
        this.config = builder.config;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ComponentRuntimeConfig from(ComponentConfig config) {
        ComponentRuntimeConfig crc = new ComponentRuntimeConfig();
        if(config == null){
            return crc;
        }
        crc.config = new ConcurrentHashMap<>(config.getConfig());
        return crc;
    }

    public ConcurrentHashMap<String, Object> getConfig() {
        return config;
    }

    public void setConfig(ConcurrentHashMap<String, Object> config) {
        this.config = config;
    }

    public <T> Optional<T> var(String key, Class<T> type) {
        Object value = config.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public boolean contains(String key) {
        return config.containsKey(key);
    }

    public void set(String key, Object value) {
        config.put(key, value);
    }

    public Optional<Boolean> varBoolean(String key) {
        return var(key, Boolean.class);
    }

    public Optional<String> varString(String key) {
        return var(key, String.class);
    }

    public Optional<Integer> varInt(String key) {
        return var(key, Integer.class);
    }

    public Optional<Double> varDouble(String key) {
        return var(key, Double.class);
    }

    public Optional<Float> varFloat(String key) {
        return var(key, Float.class);
    }

    @SuppressWarnings("rawtypes")
    public Optional<List> varList(String key) {
        return var(key, List.class);
    }

    @SuppressWarnings("rawtypes")
    public Optional<Map> varMap(String key) {
        return var(key, Map.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRuntimeConfig that = (ComponentRuntimeConfig) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(config);
    }

    public static class Builder {
        private ConcurrentHashMap<String, Object> config = new ConcurrentHashMap<>();

        public Builder withConfig(ConcurrentHashMap<String, Object> config) {
            this.config = config != null ? new ConcurrentHashMap<>(config) : new ConcurrentHashMap<>();
            return this;
        }

        public Builder withConfig(Map<String, Object> config) {
            this.config = config != null ? new ConcurrentHashMap<>(config) : new ConcurrentHashMap<>();
            return this;
        }

        public Builder put(String key, Object value) {
            if (key != null) {
                this.config.put(key, value);
            }
            return this;
        }

        public Builder putAll(Map<String, Object> values) {
            if (values != null) {
                this.config.putAll(values);
            }
            return this;
        }

        public Builder putIfAbsent(String key, Object value) {
            if (key != null) {
                this.config.putIfAbsent(key, value);
            }
            return this;
        }

        public Builder remove(String key) {
            if (key != null) {
                this.config.remove(key);
            }
            return this;
        }

        public Builder clear() {
            this.config.clear();
            return this;
        }

        public ComponentRuntimeConfig build() {
            return new ComponentRuntimeConfig(this);
        }
    }
}
