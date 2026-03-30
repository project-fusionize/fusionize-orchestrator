package dev.fusionize.worker.storage;

import dev.fusionize.storage.StorageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigLoaderTest {

    private final StorageConfigLoader storageConfigLoader = new StorageConfigLoader();

    @Test
    void shouldReturnEmptyList_whenDirNotExists() throws IOException {
        // setup
        var nonExistentPath = "/tmp/non-existent-dir-" + System.nanoTime();

        // expectation
        List<StorageConfig> result = storageConfigLoader.loadStorageConfigs(nonExistentPath);

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenNotDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        File tempFile = tempDir.resolve("not-a-directory.txt").toFile();
        tempFile.createNewFile();

        // expectation
        List<StorageConfig> result = storageConfigLoader.loadStorageConfigs(tempFile.getAbsolutePath());

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldLoadStorageConfigsFromDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        String yamlContent = """
                name: test-storage
                domain: test-domain
                provider: MONGO_DB
                storageType: VECTOR_STORAGE
                enabled: true
                """;
        Path storageFile = tempDir.resolve("storage.yml");
        Files.writeString(storageFile, yamlContent);

        // expectation
        List<StorageConfig> result = storageConfigLoader.loadStorageConfigs(tempDir.toString());

        // validation
        assertThat(result).isNotEmpty();
    }
}
