package dev.fusionize.worker.loader;

import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.worker.ai.AgentConfigLoader;
import dev.fusionize.worker.ai.ChatModelConfigLoader;
import dev.fusionize.worker.storage.StorageConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigLoaderTest {

    @Test
    void shouldParseAgentConfigYaml() {
        String yaml = "kind: AgentConfig\n" +
                "apiVersion: v1\n" +
                "name: test-agent\n" +
                "domain: test.agent\n" +
                "role: ANALYZER\n" +
                "instructionPrompt: \"You are a test agent\"";
        
        dev.fusionize.ai.model.descriptor.AgentConfigDescriptor descriptor = new dev.fusionize.ai.model.descriptor.AgentConfigDescriptor();
        AgentConfig config = descriptor.fromYamlDescription(yaml);
        
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("test-agent");
        assertThat(config.getRole()).isEqualTo(AgentConfig.Role.ANALYZER);
    }

    @Test
    void shouldLoadAgentConfigs(@TempDir Path tempDir) throws IOException {
        String yaml = "kind: AgentConfig\n" +
                "apiVersion: v1\n" +
                "name: test-agent\n" +
                "domain: test.agent\n" +
                "role: ANALYZER\n" +
                "instructionPrompt: \"You are a test agent\"";
        Path path = tempDir.resolve("agent.yaml");
        Files.writeString(path, yaml);
        
        System.out.println("Created file at: " + path.toAbsolutePath());
        System.out.println("File exists: " + Files.exists(path));

        AgentConfigLoader loader = new AgentConfigLoader();
        List<AgentConfig> configs = loader.loadAgentConfigs(tempDir.toString());

        assertThat(configs).withFailMessage("Expected 1 config but got " + configs.size()).hasSize(1);
        AgentConfig config = configs.get(0);
        assertThat(config.getName()).isEqualTo("test-agent");
        assertThat(config.getRole()).isEqualTo(AgentConfig.Role.ANALYZER);
        assertThat(config.getInstructionPrompt()).isEqualTo("You are a test agent");
    }

    @Test
    void shouldLoadChatModelConfigs(@TempDir Path tempDir) throws IOException {
        String yaml = "kind: ChatModelConfig\n" +
                "apiVersion: v1\n" +
                "name: test-model\n" +
                "domain: test.model\n" +
                "provider: openai\n" +
                "modelName: gpt-4";
        Files.writeString(tempDir.resolve("chat-model.yaml"), yaml);

        ChatModelConfigLoader loader = new ChatModelConfigLoader();
        List<ChatModelConfig> configs = loader.loadChatModelConfigs(tempDir.toString());

        assertThat(configs).hasSize(1);
        ChatModelConfig config = configs.get(0);
        assertThat(config.getName()).isEqualTo("test-model");
        assertThat(config.getProvider()).isEqualTo("openai");
        assertThat(config.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void shouldLoadStorageConfigs(@TempDir Path tempDir) throws IOException {
        String yaml = "kind: StorageConfig\n" +
                "apiVersion: v1\n" +
                "name: test-storage\n" +
                "domain: test.storage\n" +
                "enabled: true";
        Files.writeString(tempDir.resolve("storage.yaml"), yaml);

        StorageConfigLoader loader = new StorageConfigLoader();
        List<StorageConfig> configs = loader.loadStorageConfigs(tempDir.toString());

        assertThat(configs).hasSize(1);
        StorageConfig config = configs.get(0);
        assertThat(config.getName()).isEqualTo("test-storage");
        assertThat(config.isEnabled()).isTrue();
    }
}
