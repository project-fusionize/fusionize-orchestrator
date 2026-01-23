package dev.fusionize.storage.descriptor;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageProvider;
import dev.fusionize.storage.StorageType;

public class StorageConfigTransformer {

    public StorageConfig toStorageConfig(StorageConfigDescription description) {
        if (description == null) {
            return null;
        }
        StorageConfig config = new StorageConfig();
        config.setName(description.getName());
        config.setDomain(description.getDomain());
        if (description.getProvider() != null) {
            config.setProvider(StorageProvider.valueOf(description.getProvider().toUpperCase()));
        }
        if (description.getStorageType() != null) {
            config.setStorageType(StorageType.valueOf(description.getStorageType().toUpperCase()));
        }
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
        if (config.getProvider() != null) {
            description.setProvider(config.getProvider().name());
        }
        if (config.getStorageType() != null) {
            description.setStorageType(config.getStorageType().name());
        }
        description.setSecrets(config.getSecrets());
        description.setProperties(config.getProperties());
        description.setEnabled(config.isEnabled());
        return description;
    }
}
