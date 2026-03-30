package dev.fusionize.worker.ai;

import dev.fusionize.ai.model.ChatModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelConfigLoaderTest {

    private final ChatModelConfigLoader chatModelConfigLoader = new ChatModelConfigLoader();

    @Test
    void shouldReturnEmptyList_whenDirNotExists() throws IOException {
        // setup
        var nonExistentPath = "/tmp/non-existent-dir-" + System.nanoTime();

        // expectation
        List<ChatModelConfig> result = chatModelConfigLoader.loadChatModelConfigs(nonExistentPath);

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenNotDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        File tempFile = tempDir.resolve("not-a-directory.txt").toFile();
        tempFile.createNewFile();

        // expectation
        List<ChatModelConfig> result = chatModelConfigLoader.loadChatModelConfigs(tempFile.getAbsolutePath());

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldLoadChatModelConfigsFromDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        String yamlContent = """
                name: test-model
                domain: test-model-domain
                provider: openai
                modelName: gpt-4
                """;
        Path chatModelFile = tempDir.resolve("chat-model.yml");
        Files.writeString(chatModelFile, yamlContent);

        // expectation
        List<ChatModelConfig> result = chatModelConfigLoader.loadChatModelConfigs(tempDir.toString());

        // validation
        assertThat(result).isNotEmpty();
    }
}
