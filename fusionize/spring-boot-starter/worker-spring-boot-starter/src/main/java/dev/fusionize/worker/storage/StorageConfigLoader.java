package dev.fusionize.worker.storage;

import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.descriptor.StorageConfigDescriptor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StorageConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(StorageConfigLoader.class);

    public List<StorageConfig> loadStorageConfigs(String storageConfigDefinitionsDir) throws IOException {
        File storageConfigFolder = new File(storageConfigDefinitionsDir);
        if(!storageConfigFolder.exists() || !storageConfigFolder.isDirectory()){
            logger.error("Worker Storage Config Definitions folder not found:  {}", storageConfigDefinitionsDir);
            return new ArrayList<>();
        }
        List<StorageConfig> storageConfigs = new ArrayList<>();
        logger.info("Scanning for Storage Config Definitions:  {}", Path.of(storageConfigFolder.getAbsolutePath()).normalize());
        FileUtils.listFiles(storageConfigFolder, new String[]{"yml","yaml"}, true)
                .stream().filter(f-> f!=null && f.exists() && f.isFile() && (f.getName().equals("storage.yml") || f.getName().equals("storage.yaml"))).forEach(file -> {
                    try {
                        StorageConfig storageConfig = new StorageConfigDescriptor().fromYamlDescription(Files.readString(file.toPath()));
                        storageConfigs.add(storageConfig);
                    } catch (Exception e) {
                        logger.error("Failed to load storage config: {} -> {}", file.getAbsolutePath(), e.getMessage());
                    }
                });
        return storageConfigs;
    }
}
