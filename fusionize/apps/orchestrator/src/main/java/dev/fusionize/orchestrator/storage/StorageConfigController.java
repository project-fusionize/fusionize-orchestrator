package dev.fusionize.orchestrator.storage;

import dev.fusionize.Application;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.exception.StorageException;
import dev.fusionize.storage.exception.StorageNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Application.API_PREFIX + "/storage-config")
public class StorageConfigController {

    private final StorageConfigManager storageConfigManager;

    public StorageConfigController(StorageConfigManager storageConfigManager) {
        this.storageConfigManager = storageConfigManager;
    }

    @GetMapping
    public ServicePayload<List<StorageConfig>> getAll(
            @RequestParam(required = false, defaultValue = "") String domain) {
        List<StorageConfig> configs = storageConfigManager.getAll(domain).stream().map(StorageConfig::sanitize)
                .toList();
        return new ServicePayload.Builder<List<StorageConfig>>()
                .response(new ServiceResponse.Builder<List<StorageConfig>>()
                        .status(200)
                        .message(configs)
                        .build())
                .build();
    }

    @GetMapping("/{domain}")
    public ServicePayload<StorageConfig> get(@PathVariable String domain) throws StorageException {
        StorageConfig config = storageConfigManager.getConfig(domain)
                .orElseThrow(() -> new StorageNotFoundException(domain));
        return new ServicePayload.Builder<StorageConfig>()
                .response(new ServiceResponse.Builder<StorageConfig>()
                        .status(200)
                        .message(config.sanitize())
                        .build())
                .build();
    }

    @PostMapping
    public ServicePayload<StorageConfig> create(@RequestBody StorageConfig config) throws StorageException {
        StorageConfig saved = storageConfigManager.createConfig(config);
        return new ServicePayload.Builder<StorageConfig>()
                .response(new ServiceResponse.Builder<StorageConfig>()
                        .status(200)
                        .message(saved.sanitize())
                        .build())
                .build();
    }

    @PutMapping("/{domain}")
    public ServicePayload<StorageConfig> update(@PathVariable String domain, @RequestBody StorageConfig config)
            throws StorageException {
        StorageConfig existing = storageConfigManager.getConfig(domain)
                .orElseThrow(() -> new StorageNotFoundException(domain));

        // Ensure the ID and domain are preserved/updated correctly
        config.setId(existing.getId());
        config.setDomain(domain);

        StorageConfig saved = storageConfigManager.saveConfig(config);
        return new ServicePayload.Builder<StorageConfig>()
                .response(new ServiceResponse.Builder<StorageConfig>()
                        .status(200)
                        .message(saved.sanitize())
                        .build())
                .build();
    }

    @DeleteMapping("/{domain}")
    public ServicePayload<Void> delete(@PathVariable String domain) {
        storageConfigManager.deleteConfig(domain);
        return new ServicePayload.Builder<Void>()
                .response(new ServiceResponse.Builder<Void>()
                        .status(200)
                        .build())
                .build();
    }

    @PostMapping("/test-connection")
    public ServicePayload<Void> testConnection(@RequestBody StorageConfig config) throws StorageException {
        storageConfigManager.testConnection(config);
        return new ServicePayload.Builder<Void>()
                .response(new ServiceResponse.Builder<Void>()
                        .status(200)
                        .build())
                .build();
    }
}
