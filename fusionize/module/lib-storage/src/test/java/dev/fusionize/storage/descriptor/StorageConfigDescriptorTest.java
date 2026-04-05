package dev.fusionize.storage.descriptor;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageProvider;
import dev.fusionize.storage.StorageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigDescriptorTest {

    private final StorageConfigDescriptor descriptor = new StorageConfigDescriptor();

    @Test
    void shouldParseYamlToStorageConfig() {
        // setup
        String yaml = """
                name: test-storage
                domain: test-domain
                provider: MONGO_DB
                storageType: VECTOR_STORAGE
                enabled: true
                """;

        // expectation
        StorageConfig config = descriptor.fromYamlDescription(yaml);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("test-storage");
        assertThat(config.getDomain()).isEqualTo("test-domain");
        assertThat(config.getProvider()).isEqualTo(StorageProvider.MONGO_DB);
        assertThat(config.getStorageType()).isEqualTo(StorageType.VECTOR_STORAGE);
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    void shouldSerializeStorageConfigToYaml() {
        // setup
        var config = new StorageConfig();
        config.setName("my-storage");
        config.setDomain("my-domain");
        config.setProvider(StorageProvider.LOCAL);
        config.setStorageType(StorageType.FILE_STORAGE);
        config.setEnabled(false);

        // expectation
        String yaml = descriptor.toYamlDescription(config);

        // validation
        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("my-storage");
        assertThat(yaml).contains("my-domain");
        assertThat(yaml).contains("LOCAL");
        assertThat(yaml).contains("FILE_STORAGE");
    }
}
