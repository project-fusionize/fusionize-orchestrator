package dev.fusionize.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTest {

    @Test
    void shouldBuildStorageConfigUsingBuilder() {
        // Given
        String parentDomain = "test-domain";
        String name = "config";
        String expectedDomain = "test-domain.config";
        StorageProvider provider = StorageProvider.AWS_S3;
        StorageType type = StorageType.FILE_STORAGE;
        Map<String, Object> secrets = new HashMap<>();
        secrets.put("accessKey", "123");
        Map<String, Object> properties = new HashMap<>();
        properties.put("bucket", "my-bucket");

        // When
        StorageConfig config = StorageConfig.builder(parentDomain)
                .withName(name)
                .withProvider(provider)
                .withStorageType(type)
                .withSecrets(secrets)
                .withProperties(properties)
                .withEnabled(false)
                .build();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getDomain()).isEqualTo(expectedDomain);
        assertThat(config.getProvider()).isEqualTo(provider);
        assertThat(config.getStorageType()).isEqualTo(type);
        assertThat(config.getSecrets()).containsEntry("accessKey", "123");
        assertThat(config.getProperties()).containsEntry("bucket", "my-bucket");
        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    void shouldAddSecretsAndPropertiesIndividually() {
        // Given
        String parentDomain = "test-domain-2";
        String name = "config";
        String expectedDomain = "test-domain-2.config";

        // When
        StorageConfig config = StorageConfig.builder(parentDomain)
                .withName(name)
                .addSecret("apiKey", "xyz")
                .addProperty("region", "us-east-1")
                .build();

        // Then
        assertThat(config.getDomain()).isEqualTo(expectedDomain);
        assertThat(config.getSecrets()).containsEntry("apiKey", "xyz");
        assertThat(config.getProperties()).containsEntry("region", "us-east-1");
        assertThat(config.isEnabled()).isTrue(); // default
    }
}
