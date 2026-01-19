package dev.fusionize.storage.descriptor;

import dev.fusionize.storage.StorageConfig;

public class StorageConfigTransformer {

    public StorageConfig toStorageConfig(StorageConfigDescription description) {
        if (description == null) {
            return null;
        }
        StorageConfig config = new StorageConfig();
        config.setName(description.getName());
        config.setDomain(description.getDomain());
        config.setProvider(description.getProvider());
        config.setStorageType(description.getStorageType());
        config.setSecrets(description.getSecrets());
        config.setProperties(description.getProperties());
        config.setEnabled(description.isEnabled());
        return config;
    }

    public StorageConfigDescription toStorageConfigDescription(StorageConfig config) {
        if (config == null) {
            return null;
        }
        StorageConfigDescription description = new StorageConfigDescription();
        description.setName(config.getName());
        description.setDomain(config.getDomain());
        description.setProvider(config.getProvider());
        description.setStorageType(config.getStorageType());
        description.setSecrets(config.getSecrets());
        description.setProperties(config.getProperties());
        description.setEnabled(config.isEnabled());
        return description;
    }
}
