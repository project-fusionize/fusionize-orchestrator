package dev.fusionize.storage;

import dev.fusionize.storage.exception.StorageDomainAlreadyExistsException;
import dev.fusionize.storage.exception.StorageException;
import dev.fusionize.storage.repo.StorageConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageConfigManagerAdditionalTest {

    @Mock
    private StorageConfigRepository repository;

    private StorageConfigManager manager;

    @BeforeEach
    void setUp() {
        manager = new StorageConfigManager(repository);
    }

    private StorageConfig validConfig() {
        var config = new StorageConfig();
        config.setDomain("test-domain.my-config");
        config.setProvider(StorageProvider.AWS_S3);
        config.setStorageType(StorageType.FILE_STORAGE);
        return config;
    }

    @Test
    void shouldCreateConfig_successfully() throws StorageException {
        // setup
        var config = validConfig();
        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.empty());
        when(repository.save(any(StorageConfig.class))).thenReturn(config);

        // expectation
        var result = manager.createConfig(config);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getDomain()).isEqualTo("test-domain.my-config");
        verify(repository).findByDomain(config.getDomain());
        verify(repository).save(config);
    }

    @Test
    void shouldThrowDomainAlreadyExists_onCreateConfig() {
        // setup
        var config = validConfig();
        var existing = validConfig();
        existing.setId("existing-id");
        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(existing));

        // expectation / validation
        assertThatThrownBy(() -> manager.createConfig(config))
                .isInstanceOf(StorageDomainAlreadyExistsException.class)
                .hasMessageContaining(config.getDomain());
    }

    @Test
    void shouldThrow_whenConfigNull() {
        // setup
        StorageConfig nullConfig = null;

        // expectation / validation
        assertThatThrownBy(() -> manager.createConfig(nullConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void shouldThrow_whenDomainBlank() {
        // setup
        var config = new StorageConfig();
        config.setDomain("");
        config.setProvider(StorageProvider.AWS_S3);
        config.setStorageType(StorageType.FILE_STORAGE);

        // expectation / validation
        assertThatThrownBy(() -> manager.createConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Domain");
    }

    @Test
    void shouldThrow_whenProviderNull() {
        // setup
        var config = new StorageConfig();
        config.setDomain("test-domain.my-config");
        config.setProvider(null);
        config.setStorageType(StorageType.FILE_STORAGE);

        // expectation / validation
        assertThatThrownBy(() -> manager.createConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider");
    }

    @Test
    void shouldThrow_whenStorageTypeNull() {
        // setup
        var config = new StorageConfig();
        config.setDomain("test-domain.my-config");
        config.setProvider(StorageProvider.AWS_S3);
        config.setStorageType(null);

        // expectation / validation
        assertThatThrownBy(() -> manager.createConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Storage Type");
    }

    @Test
    void shouldSaveConfig_withExistingDomain() throws StorageException {
        // setup
        var config = validConfig();
        config.setId(null);

        var existing = validConfig();
        existing.setId("existing-id");

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(existing));
        when(repository.save(any(StorageConfig.class))).thenReturn(config);

        // expectation
        var result = manager.saveConfig(config);

        // validation
        assertThat(result).isNotNull();
        assertThat(config.getId()).isEqualTo("existing-id");
        verify(repository).save(config);
    }

    @Test
    void shouldThrowDomainAlreadyExists_whenIdMismatch() {
        // setup
        var config = validConfig();
        config.setId("different-id");

        var existing = validConfig();
        existing.setId("existing-id");

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(existing));

        // expectation / validation
        assertThatThrownBy(() -> manager.saveConfig(config))
                .isInstanceOf(StorageDomainAlreadyExistsException.class)
                .hasMessageContaining(config.getDomain());
    }

    @Test
    void shouldGetConfig() {
        // setup
        var config = validConfig();
        when(repository.findByDomain("test-domain.my-config")).thenReturn(Optional.of(config));

        // expectation
        var result = manager.getConfig("test-domain.my-config");

        // validation
        assertThat(result).isPresent();
        assertThat(result.get().getDomain()).isEqualTo("test-domain.my-config");
        verify(repository).findByDomain("test-domain.my-config");
    }

    @Test
    void shouldDeleteConfig() {
        // setup
        var domain = "test-domain.my-config";

        // expectation
        manager.deleteConfig(domain);

        // validation
        verify(repository).deleteByDomain(domain);
    }

    @Test
    void shouldGetAll() {
        // setup
        var config1 = validConfig();
        var config2 = validConfig();
        config2.setDomain("test-domain.other");
        when(repository.findByDomainStartingWith("test-domain")).thenReturn(List.of(config1, config2));

        // expectation
        var result = manager.getAll("test-domain");

        // validation
        assertThat(result).hasSize(2);
        verify(repository).findByDomainStartingWith("test-domain");
    }

    @Test
    void shouldSplitPropertiesIntoSecrets() throws StorageException {
        // setup
        var config = validConfig();
        config.setProperties(Map.of(
                "bucketName", "my-bucket",
                "accessKey", "secret-access-key",
                "secretkey", "secret-value",
                "region", "us-east-1"
        ));
        config.setSecrets(new java.util.HashMap<>());

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.empty());
        when(repository.save(any(StorageConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // expectation
        var result = manager.createConfig(config);

        // validation
        assertThat(result.getProperties())
                .containsKey("bucketName")
                .containsKey("region")
                .doesNotContainKey("accessKey")
                .doesNotContainKey("secretkey");
        assertThat(result.getSecrets())
                .containsEntry("accessKey", "secret-access-key")
                .containsEntry("secretkey", "secret-value");
    }
}
