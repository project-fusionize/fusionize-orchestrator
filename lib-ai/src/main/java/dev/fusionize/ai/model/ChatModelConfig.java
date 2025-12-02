package dev.fusionize.ai.model;

import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "ai_chat_model_config")
public class ChatModelConfig extends DomainEntity {

    private String provider;
    private String apiKey;
    private Map<String, Object> properties = new HashMap<>();
    private Set<String> capabilities = new HashSet<>();
    private String modelName;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Double getTemperature() {
        return (Double) properties.get("temperature");
    }

    public void setTemperature(Double temperature) {
        this.properties.put("temperature", temperature);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private String provider;
        private String apiKey;
        private Map<String, Object> properties = new HashMap<>();
        private Set<String> capabilities = new HashSet<>();
        private String modelName;

        private Builder(String parentDomain) {
            super(parentDomain);
        }

        public Builder withProvider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder withTemperature(Double temperature) {
            this.properties.put("temperature", temperature);
            return this;
        }

        public Builder withProperties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public Builder addProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder withCapabilities(Set<String> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder addCapability(String capability) {
            this.capabilities.add(capability);
            return this;
        }

        public Builder withModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        @Override
        public ChatModelConfig build() {
            ChatModelConfig config = new ChatModelConfig();
            config.load(super.build());
            config.setProvider(this.provider);
            config.setApiKey(this.apiKey);
            config.setProperties(this.properties);
            config.setCapabilities(this.capabilities);
            config.setModelName(this.modelName);
            return config;
        }
    }
}
