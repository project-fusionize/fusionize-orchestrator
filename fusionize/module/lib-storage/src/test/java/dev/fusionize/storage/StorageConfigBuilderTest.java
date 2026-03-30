package dev.fusionize.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigBuilderTest {

    @Test
    void shouldBuildWithAllFields() {
        // setup
        var secrets = Map.<String, Object>of("apiKey", "secret-123");
        var properties = Map.<String, Object>of("bucket", "my-bucket");

        // expectation
        var config = StorageConfig.builder("parent-domain")
                .withName("my-storage")
                .withProvider(StorageProvider.AWS_S3)
                .withStorageType(StorageType.FILE_STORAGE)
                .withSecrets(secrets)
                .withProperties(properties)
                .withEnabled(false)
                .build();

        // validation
        assertThat(config.getProvider()).isEqualTo(StorageProvider.AWS_S3);
        assertThat(config.getStorageType()).isEqualTo(StorageType.FILE_STORAGE);
        assertThat(config.getSecrets()).containsEntry("apiKey", "secret-123");
        assertThat(config.getProperties()).containsEntry("bucket", "my-bucket");
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getName()).isEqualTo("my-storage");
        assertThat(config.getDomain()).contains("parent-domain");
    }

    @Test
    void shouldAddSecret() {
        // setup / expectation
        var config = StorageConfig.builder("parent-domain")
                .withName("storage")
                .withProvider(StorageProvider.PINECONE)
                .withStorageType(StorageType.VECTOR_STORAGE)
                .addSecret("apiKey", "key-1")
                .addSecret("apiSecret", "secret-1")
                .build();

        // validation
        assertThat(config.getSecrets())
                .hasSize(2)
                .containsEntry("apiKey", "key-1")
                .containsEntry("apiSecret", "secret-1");
    }

    @Test
    void shouldAddProperty() {
        // setup / expectation
        var config = StorageConfig.builder("parent-domain")
                .withName("storage")
                .withProvider(StorageProvider.LOCAL)
                .withStorageType(StorageType.FILE_STORAGE)
                .addProperty("path", "/tmp/storage")
                .addProperty("maxSize", 1024)
                .build();

        // validation
        assertThat(config.getProperties())
                .hasSize(2)
                .containsEntry("path", "/tmp/storage")
                .containsEntry("maxSize", 1024);
    }

    @Test
    void shouldDefaultEnabledToTrue() {
        // setup / expectation
        var config = StorageConfig.builder("parent-domain")
                .withName("storage")
                .withProvider(StorageProvider.AWS_S3)
                .withStorageType(StorageType.FILE_STORAGE)
                .build();

        // validation
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    void shouldSanitizeHidesSecrets() {
        // setup
        var config = StorageConfig.builder("parent-domain")
                .withName("storage")
                .withProvider(StorageProvider.AWS_S3)
                .withStorageType(StorageType.FILE_STORAGE)
                .addSecret("apiKey", "super-secret")
                .addProperty("bucket", "my-bucket")
                .withEnabled(true)
                .build();

        // expectation
        var sanitized = config.sanitize();

        // validation
        assertThat(sanitized.getSecrets()).isEmpty();
        assertThat(sanitized.getProperties()).containsEntry("bucket", "my-bucket");
        assertThat(sanitized.getProvider()).isEqualTo(StorageProvider.AWS_S3);
        assertThat(sanitized.getStorageType()).isEqualTo(StorageType.FILE_STORAGE);
        assertThat(sanitized.isEnabled()).isTrue();
    }
}
