package dev.fusionize.workflow.component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowComponentConfig {
    private ConcurrentHashMap<String, Object> config = new ConcurrentHashMap<>();

    public WorkflowComponentConfig() {
    }

    private WorkflowComponentConfig(Builder builder) {
        this.config = builder.config;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConcurrentHashMap<String, Object> getConfig() {
        return config;
    }

    public void setConfig(ConcurrentHashMap<String, Object> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowComponentConfig that = (WorkflowComponentConfig) o;
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

        public WorkflowComponentConfig build() {
            return new WorkflowComponentConfig(this);
        }
    }
}
