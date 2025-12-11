package dev.fusionize.web.services;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.file.FileStorageService;
import org.springframework.stereotype.Component;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class FileInboundConnectorService {
    private final StorageConfigManager configManager;
    Map<IngestKey, Consumer<MultipartFile>> listeners = new ConcurrentHashMap<>();

    public FileInboundConnectorService(StorageConfigManager configManager) {
        this.configManager = configManager;
    }

    public record IngestKey(String workflowKey, String workflowNodeKey) {
        @Override
        public int hashCode() {
            return Objects.hash(workflowKey, workflowNodeKey);
        }
    }

    public FileStorageService getFileStorageService(String storageDomain) {
        Optional<StorageConfig> configOptional =  this.configManager.getConfig(storageDomain);
        return configOptional.map(this.configManager::getFileStorageService).orElse(null);
    }


    public void addListener(IngestKey key, Consumer<MultipartFile> listener) {
        listeners.put(key, listener);
    }

    public void removeListener(IngestKey key) {
        listeners.remove(key);
    }

    public void invoke(IngestKey key, MultipartFile file) {
        Consumer<MultipartFile> listener = listeners.get(key);
        if (listener != null) {
            listener.accept(file);
        }
    }
}
