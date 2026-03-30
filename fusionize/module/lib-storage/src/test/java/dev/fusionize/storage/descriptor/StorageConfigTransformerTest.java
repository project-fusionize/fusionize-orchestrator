package dev.fusionize.storage.descriptor;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageProvider;
import dev.fusionize.storage.StorageType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTransformerTest {

    private final StorageConfigTransformer transformer = new StorageConfigTransformer();

    @Test
    void shouldConvertDescriptionToConfig() {
        // setup
        var description = new StorageConfigDescription();
        description.setName("my-storage");
        description.setDomain("my-domain");
        description.setProvider("MONGO_DB");
        description.setStorageType("VECTOR_STORAGE");
        description.setSecrets(Map.of("key", "secret-value"));
        description.setProperties(Map.of("prop1", "value1"));
        description.setEnabled(true);

        // expectation
        StorageConfig config = transformer.toStorageConfig(description);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("my-storage");
        assertThat(config.getDomain()).isEqualTo("my-domain");
        assertThat(config.getProvider()).isEqualTo(StorageProvider.MONGO_DB);
        assertThat(config.getStorageType()).isEqualTo(StorageType.VECTOR_STORAGE);
        assertThat(config.getSecrets()).containsEntry("key", "secret-value");
        assertThat(config.getProperties()).containsEntry("prop1", "value1");
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    void shouldReturnNull_forNullDescription() {
        // setup
        StorageConfigDescription description = null;

        // expectation
        StorageConfig config = transformer.toStorageConfig(description);

        // validation
        assertThat(config).isNull();
    }

    @Test
    void shouldHandleNullProvider() {
        // setup
        var description = new StorageConfigDescription();
        description.setName("test");
        description.setDomain("test-domain");
        description.setProvider(null);
        description.setStorageType("FILE_STORAGE");

        // expectation
        StorageConfig config = transformer.toStorageConfig(description);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getProvider()).isNull();
        assertThat(config.getStorageType()).isEqualTo(StorageType.FILE_STORAGE);
    }

    @Test
    void shouldHandleNullStorageType() {
        // setup
        var description = new StorageConfigDescription();
        description.setName("test");
        description.setDomain("test-domain");
        description.setProvider("LOCAL");
        description.setStorageType(null);

        // expectation
        StorageConfig config = transformer.toStorageConfig(description);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getProvider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(config.getStorageType()).isNull();
    }

    @Test
    void shouldConvertConfigToDescription() {
        // setup
        var config = new StorageConfig();
        config.setName("my-storage");
        config.setDomain("my-domain");
        config.setProvider(StorageProvider.PINECONE);
        config.setStorageType(StorageType.VECTOR_STORAGE);
        config.setSecrets(Map.of("secret-key", "secret-val"));
        config.setProperties(Map.of("prop-key", "prop-val"));
        config.setEnabled(false);

        // expectation
        StorageConfigDescription description = transformer.toStorageConfigDescription(config);

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getName()).isEqualTo("my-storage");
        assertThat(description.getDomain()).isEqualTo("my-domain");
        assertThat(description.getProvider()).isEqualTo("PINECONE");
        assertThat(description.getStorageType()).isEqualTo("VECTOR_STORAGE");
        assertThat(description.getSecrets()).containsEntry("secret-key", "secret-val");
        assertThat(description.getProperties()).containsEntry("prop-key", "prop-val");
        assertThat(description.isEnabled()).isFalse();
    }

    @Test
    void shouldReturnNull_forNullConfig() {
        // setup
        StorageConfig config = null;

        // expectation
        StorageConfigDescription description = transformer.toStorageConfigDescription(config);

        // validation
        assertThat(description).isNull();
    }

    @Test
    void shouldHandleNullProviderInConfig() {
        // setup
        var config = new StorageConfig();
        config.setName("test");
        config.setDomain("test-domain");
        config.setProvider(null);
        config.setStorageType(StorageType.FILE_STORAGE);

        // expectation
        StorageConfigDescription description = transformer.toStorageConfigDescription(config);

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getProvider()).isNull();
        assertThat(description.getStorageType()).isEqualTo("FILE_STORAGE");
    }
}
