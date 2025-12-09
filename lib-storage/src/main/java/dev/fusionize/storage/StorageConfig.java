package dev.fusionize.storage;

import dev.fusionize.common.parser.JsonParser;
import dev.fusionize.common.sanitizer.Sanitization;
import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "storage_config")
public class StorageConfig extends DomainEntity implements Sanitization<StorageConfig> {
    @Id
    private String id;
    private StorageProvider provider;
    private StorageType storageType;
    private Map<String, Object> secrets = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private boolean enabled = true;

    @Override
    public StorageConfig sanitize() {
        JsonParser<StorageConfig> parser = new JsonParser<>();
        StorageConfig replica = parser.fromJson(parser.toJson(this, StorageConfig.class), StorageConfig.class);
        replica.setSecrets(new HashMap<>());
        return replica;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public Map<String, Object> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, Object> secrets) {
        this.secrets = secrets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private StorageProvider provider;
        private StorageType storageType;
        private Map<String, Object> secrets = new HashMap<>();
        private Map<String, Object> properties = new HashMap<>();
        private boolean enabled = true;

        private Builder(String parentDomain) {
            super(parentDomain);
        }

        public Builder withProvider(StorageProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder withStorageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

        public Builder withSecrets(Map<String, Object> secrets) {
            this.secrets = secrets;
            return this;
        }

        public Builder addSecret(String key, Object value) {
            this.secrets.put(key, value);
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

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public StorageConfig build() {
            StorageConfig config = new StorageConfig();
            config.load(super.build());
            config.setProvider(this.provider);
            config.setStorageType(this.storageType);
            config.setSecrets(this.secrets);
            config.setProperties(this.properties);
            config.setEnabled(this.enabled);
            return config;
        }
    }
}
