package dev.fusionize.worker.ai;

import dev.fusionize.ai.model.AgentConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigLoaderTest {

    private final AgentConfigLoader agentConfigLoader = new AgentConfigLoader();

    @Test
    void shouldReturnEmptyList_whenDirNotExists() throws IOException {
        // setup
        var nonExistentPath = "/tmp/non-existent-dir-" + System.nanoTime();

        // expectation
        List<AgentConfig> result = agentConfigLoader.loadAgentConfigs(nonExistentPath);

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenNotDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        File tempFile = tempDir.resolve("not-a-directory.txt").toFile();
        tempFile.createNewFile();

        // expectation
        List<AgentConfig> result = agentConfigLoader.loadAgentConfigs(tempFile.getAbsolutePath());

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldLoadAgentConfigsFromDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        String yamlContent = """
                name: test-agent
                domain: test-agent-domain
                role: ANALYZER
                """;
        Path agentFile = tempDir.resolve("agent.yml");
        Files.writeString(agentFile, yamlContent);

        // expectation
        List<AgentConfig> result = agentConfigLoader.loadAgentConfigs(tempDir.toString());

        // validation
        assertThat(result).isNotEmpty();
    }
}
