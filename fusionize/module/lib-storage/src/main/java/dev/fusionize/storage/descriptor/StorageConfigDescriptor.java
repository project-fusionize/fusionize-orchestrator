package dev.fusionize.storage.descriptor;

import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.common.parser.descriptor.YamlDescriptor;
import dev.fusionize.storage.StorageConfig;

public class StorageConfigDescriptor implements YamlDescriptor<StorageConfig> {

    private final YamlParser<StorageConfigDescription> yamlParser = new YamlParser<>();

    @Override
    public StorageConfig fromYamlDescription(String yaml) {
        StorageConfigDescription description = yamlParser.fromYaml(yaml, StorageConfigDescription.class);
        return new StorageConfigTransformer().toStorageConfig(description);
    }

    @Override
    public String toYamlDescription(StorageConfig model) {
        StorageConfigDescription description = new StorageConfigTransformer().toStorageConfigDescription(model);
        return yamlParser.toYaml(description);
    }
}
