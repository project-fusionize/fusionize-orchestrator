package dev.fusionize.storage.descriptor;

import dev.fusionize.common.parser.descriptor.Description;
import dev.fusionize.storage.StorageProvider;
import dev.fusionize.storage.StorageType;

import java.util.HashMap;
import java.util.Map;

public class StorageConfigDescription extends Description {
    private String name;
    private String domain;
    private StorageProvider provider;
    private StorageType storageType;
    private Map<String, Object> secrets = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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
}
