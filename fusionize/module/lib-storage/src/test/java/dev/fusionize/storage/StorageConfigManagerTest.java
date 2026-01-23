package dev.fusionize.storage;

import dev.fusionize.storage.exception.StorageException;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.storage.repo.StorageConfigRepository;
import dev.fusionize.storage.vector.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class StorageConfigManagerTest {

    @Mock
    private StorageConfigRepository repository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private VectorStorageService vectorStorageService;

    // Use a spy to override the get*Service methods
    private StorageConfigManager manager;

    @BeforeEach
    void setUp() {
        // We create a subclass to spy/override the methods because standard Spy might
        // not work easily with void/final methods or internal logic without more
        // complex setup
        // Actually, let's use a partial mock approach via subclassing for simplicity in
        // test
        manager = new StorageConfigManager(repository) {
            @Override
            public FileStorageService getFileStorageService(StorageConfig config) {
                return fileStorageService;
            }

            @Override
            public VectorStorageService getVectorStorageService(StorageConfig config) {
                return vectorStorageService;
            }
        };
    }

    @Test
    void testConnection_FileStorage() throws Exception {
        // Given
        StorageConfig config = StorageConfig.builder("test-domain")
                .withName("file-config")
                .withStorageType(StorageType.FILE_STORAGE)
                .withProvider(StorageProvider.AWS_S3)
                .build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(fileStorageService.write(anyList())).thenAnswer(invocation -> {
            List<String> paths = invocation.getArgument(0);
            return Map.of(paths.get(0), os);
        });
        when(fileStorageService.save(anyList())).thenReturn(Map.of("path", true));
        when(fileStorageService.remove(anyList())).thenReturn(Map.of("path", true));

        // When
        manager.testConnection(config);

        // Then
        verify(fileStorageService).write(anyList());
        verify(fileStorageService).save(anyList());
        verify(fileStorageService).remove(anyList());
        assertThat(os.toString()).isEqualTo("Test Connection");
    }

    @Test
    void testConnection_VectorStorage() throws StorageException {
        // Given
        StorageConfig config = StorageConfig.builder("test-domain")
                .withName("vector-config")
                .withStorageType(StorageType.VECTOR_STORAGE)
                .withProvider(StorageProvider.PINECONE)
                .build();

        // When
        manager.testConnection(config);

        // Then
        ArgumentCaptor<List<Document>> addCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStorageService).add(addCaptor.capture());
        assertThat(addCaptor.getValue()).hasSize(1);
        assertThat(addCaptor.getValue().get(0).getText()).isEqualTo("Test Connection");

        verify(vectorStorageService).delete(anyList());
    }
    @Test
    void createConfig_Success() throws StorageException {
        StorageConfig config = StorageConfig.builder("test-domain")
                .withName("file-config")
                .withStorageType(StorageType.FILE_STORAGE)
                .withProvider(StorageProvider.AWS_S3)
                .build();

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.empty());
        when(repository.save(any(StorageConfig.class))).thenReturn(config);

        StorageConfig saved = manager.createConfig(config);
        assertThat(saved).isNotNull();
        verify(repository).save(config);
    }

    @Test
    void createConfig_DomainExists() {
        StorageConfig config = StorageConfig.builder("test-domain")
                .withName("file-config")
                .withStorageType(StorageType.FILE_STORAGE)
                .withProvider(StorageProvider.AWS_S3)
                .build();

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(config));

        org.junit.jupiter.api.Assertions.assertThrows(dev.fusionize.storage.exception.StorageDomainAlreadyExistsException.class, () -> manager.createConfig(config));
    }

    @Test
    void saveConfig_Upsert() throws StorageException {
        StorageConfig config = StorageConfig.builder("test-domain")
                .withName("file-config")
                .withStorageType(StorageType.FILE_STORAGE)
                .withProvider(StorageProvider.AWS_S3)
                .build();
        // ID is null by default

        StorageConfig existing = StorageConfig.builder("test-domain")
                .withName("existing")
                .withStorageType(StorageType.FILE_STORAGE)
                .withProvider(StorageProvider.AWS_S3)
                .build();
        existing.setId("existing-id");

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(existing));
        when(repository.save(any(StorageConfig.class))).thenReturn(config);

        StorageConfig saved = manager.saveConfig(config);

        assertThat(config.getId()).isEqualTo("existing-id");
        verify(repository).save(config);
    }
}
