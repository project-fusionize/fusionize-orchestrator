package dev.fusionize.storage;

import dev.fusionize.storage.exception.StorageConnectionException;
import dev.fusionize.storage.exception.StorageDomainAlreadyExistsException;
import dev.fusionize.storage.exception.StorageException;

import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.storage.repo.StorageConfigRepository;
import dev.fusionize.storage.vector.VectorStorageService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class StorageConfigManager {

    private final StorageConfigRepository repository;

    public StorageConfigManager(StorageConfigRepository repository) {
        this.repository = repository;
    }

    public StorageConfig saveConfig(StorageConfig config) throws StorageException {
        validateConfig(config);

        if (config.getId() == null && repository.findByDomain(config.getDomain()).isPresent()) {
            throw new StorageDomainAlreadyExistsException(config.getDomain());
        }

        if (config.getId() != null) {
            Optional<StorageConfig> existing = repository.findByDomain(config.getDomain());
            if (existing.isPresent() && !existing.get().getId().equals(config.getId())) {
                throw new StorageDomainAlreadyExistsException(config.getDomain());
            }
        }

        return repository.save(config);
    }

    public Optional<StorageConfig> getConfig(String domain) {
        return repository.findByDomain(domain);
    }

    public void deleteConfig(String domain) {
        repository.deleteByDomain(domain);
    }

    public List<StorageConfig> getAll(String domain) {
        return repository.findByDomainStartingWith(domain);
    }

    public FileStorageService getFileStorageService(StorageConfig config) {
        if (config.getStorageType() != StorageType.FILE_STORAGE) {
            return null;
        }
        if ("aws-s3".equalsIgnoreCase(config.getProvider())) {
            return dev.fusionize.storage.file.FileStorageServiceS3.instantiate(config, null);
        }
        return null;
    }

    public VectorStorageService getVectorStorageService(StorageConfig config) {
        if (config.getStorageType() != StorageType.VECTOR_STORAGE) {
            return null;
        }
        if ("pinecone".equalsIgnoreCase(config.getProvider()) || "pincone".equalsIgnoreCase(config.getProvider())) {
            return dev.fusionize.storage.vector.PineconeVectorStorageService.instantiate(config, null);
        } else if ("mongo".equalsIgnoreCase(config.getProvider())) {
            return dev.fusionize.storage.vector.MongoVectorStorageService.instantiate(config, null);
        }
        return null;
    }

    public void testConnection(StorageConfig config) throws StorageException {
        try {
            if (config.getStorageType() == StorageType.VECTOR_STORAGE) {
                testVectorConnection(config);
            } else if (config.getStorageType() == StorageType.FILE_STORAGE) {
                testFileConnection(config);
            }
        } catch (Exception e) {
            throw new StorageConnectionException("Failed to connect to storage provider: " + e.getMessage(), e);
        }
    }

    private void testVectorConnection(StorageConfig config) {
        VectorStorageService service = getVectorStorageService(config);
        if (service == null) {
            throw new IllegalStateException("VectorStorageService not available (implementation returned null)");
        }
        String testId = "test-connection-" + UUID.randomUUID();
        Document doc = new Document(testId, "Test Connection", Collections.emptyMap());
        service.add(List.of(doc));
        service.delete(List.of(testId));
    }

    private void testFileConnection(StorageConfig config) throws Exception {
        FileStorageService service = getFileStorageService(config);
        if (service == null) {
            throw new IllegalStateException("FileStorageService not available (implementation returned null)");
        }
        String testPath = "test-connection-" + UUID.randomUUID() + ".txt";

        // Write
        Map<String, OutputStream> streams = service.write(List.of(testPath));
        if (streams.containsKey(testPath)) {
            try (OutputStream os = streams.get(testPath)) {
                os.write("Test Connection".getBytes(StandardCharsets.UTF_8));
            }
        }

        // Save
        service.save(List.of(testPath));

        // Remove
        service.remove(List.of(testPath));
    }

    private void validateConfig(StorageConfig config) throws StorageException {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (!StringUtils.hasText(config.getDomain())) {
            throw new IllegalArgumentException("Domain is required");
        }
        if (!StringUtils.hasText(config.getProvider())) {
            throw new IllegalArgumentException("Provider is required");
        }
        if (config.getStorageType() == null) {
            throw new IllegalArgumentException("Storage Type is required");
        }
    }
}
