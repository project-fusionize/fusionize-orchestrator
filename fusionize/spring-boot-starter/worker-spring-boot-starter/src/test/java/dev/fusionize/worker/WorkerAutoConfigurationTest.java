package dev.fusionize.worker;

import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.service.AgentConfigManager;
import dev.fusionize.ai.service.ChatModelManager;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkerAutoConfigurationTest {

    @Test
    void shouldRegisterResources(@TempDir Path tempDir) throws Exception {
        // Arrange
        WorkerProperties properties = new WorkerProperties();
        properties.setResourceRoot(tempDir.toString());
        
        WorkerAutoConfiguration config = new WorkerAutoConfiguration(properties);
        
        Orchestrator orchestrator = mock(Orchestrator.class);
        WorkflowRegistry workflowRegistry = mock(WorkflowRegistry.class);
        AgentConfigManager agentConfigManager = mock(AgentConfigManager.class);
        ChatModelManager chatModelManager = mock(ChatModelManager.class);
        StorageConfigManager storageConfigManager = mock(StorageConfigManager.class);

        when(workflowRegistry.register(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        // Create test files
        createWorkflowFile(tempDir);
        createAgentConfigFile(tempDir);
        createChatModelConfigFile(tempDir);
        createStorageConfigFile(tempDir);

        // Act
        ApplicationRunner runner = config.registerResources(orchestrator, workflowRegistry, agentConfigManager, chatModelManager, storageConfigManager);
        runner.run(mock(ApplicationArguments.class));
        runner.run(mock(ApplicationArguments.class));

        // Assert
        verify(workflowRegistry, times(2)).register(any(Workflow.class));
        verify(agentConfigManager, times(2)).saveConfig(any(AgentConfig.class));
        verify(chatModelManager, times(2)).saveModel(any(ChatModelConfig.class));
        verify(storageConfigManager, times(2)).saveConfig(any(StorageConfig.class));
    }

    private void createWorkflowFile(Path dir) throws IOException {
        String yaml = "kind: Workflow\n" +
                "apiVersion: v1\n" +
                "name: test-workflow\n" +
                "domain: test.workflow\n" +
                "active: true\n" +
                "nodes: {}";
        Files.writeString(dir.resolve("workflow.yml"), yaml);
    }

    private void createAgentConfigFile(Path dir) throws IOException {
        String yaml = "kind: AgentConfig\n" +
                "apiVersion: v1\n" +
                "name: test-agent\n" +
                "domain: test.agent\n" +
                "role: ANALYZER\n" +
                "instructionPrompt: \"You are a test agent\"";
        Files.writeString(dir.resolve("agent.yml"), yaml);
    }

    private void createChatModelConfigFile(Path dir) throws IOException {
        String yaml = "kind: ChatModelConfig\n" +
                "apiVersion: v1\n" +
                "name: test-model\n" +
                "domain: test.model\n" +
                "provider: openai\n" +
                "modelName: gpt-4";
        Files.writeString(dir.resolve("chat-model.yml"), yaml);
    }

    private void createStorageConfigFile(Path dir) throws IOException {
        String yaml = "kind: StorageConfig\n" +
                "apiVersion: v1\n" +
                "name: test-storage\n" +
                "domain: test.storage\n" +
                "enabled: true";
        Files.writeString(dir.resolve("storage.yml"), yaml);
    }
}
