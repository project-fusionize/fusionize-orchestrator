package dev.fusionize.ai.service;

import dev.fusionize.ai.exception.*;
import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.model.McpClientConfig;
import dev.fusionize.ai.repo.AgentConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigManagerTest {

    @Mock
    private AgentConfigRepository repository;
    @Mock
    private ChatModelManager chatModelManager;
    @Mock
    private McpManager mcpManager;

    private AgentConfigManager manager;

    @BeforeEach
    void setUp() {
        manager = new AgentConfigManager(repository, chatModelManager, mcpManager);
    }

    @Test
    void saveConfig_Success() throws AgentConfigException {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");

        when(repository.findByDomain("test.agent")).thenReturn(Optional.empty());
        when(repository.save(any(AgentConfig.class))).thenReturn(config);

        AgentConfig saved = manager.saveConfig(config);
        assertNotNull(saved);
        assertEquals("test.agent", saved.getDomain());
        verify(repository).save(config);
    }

    @Test
    void saveConfig_DomainExists() {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");

        when(repository.findByDomain("test.agent")).thenReturn(Optional.of(config));

        assertThrows(AgentConfigDomainAlreadyExistsException.class, () -> manager.saveConfig(config));
    }

    @Test
    void saveConfig_InvalidConfig_Null() {
        assertThrows(InvalidAgentConfigException.class, () -> manager.saveConfig(null));
    }

    @Test
    void saveConfig_InvalidConfig_NoDomain() {
        AgentConfig config = new AgentConfig();
        assertThrows(InvalidAgentConfigException.class, () -> manager.saveConfig(config));
    }

    @Test
    void saveConfig_InvalidModelReference() {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        config.setModelConfigDomain("invalid.model");

        when(chatModelManager.getModel("invalid.model")).thenReturn(Optional.empty());

        assertThrows(InvalidAgentConfigException.class, () -> manager.saveConfig(config));
    }

    @Test
    void saveConfig_InvalidMcpToolReference() {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        config.setAllowedMcpTools(List.of("invalid.tool"));

        when(mcpManager.getConfigByDomain("invalid.tool")).thenReturn(Optional.empty());

        assertThrows(InvalidAgentConfigException.class, () -> manager.saveConfig(config));
    }

    @Test
    void saveConfig_ValidReferences() throws AgentConfigException {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        config.setModelConfigDomain("valid.model");
        config.setAllowedMcpTools(List.of("valid.tool"));

        when(repository.findByDomain("test.agent")).thenReturn(Optional.empty());
        when(chatModelManager.getModel("valid.model")).thenReturn(Optional.of(new ChatModelConfig()));
        when(mcpManager.getConfigByDomain("valid.tool")).thenReturn(Optional.of(new McpClientConfig()));
        when(repository.save(any(AgentConfig.class))).thenReturn(config);

        AgentConfig saved = manager.saveConfig(config);
        assertNotNull(saved);
        verify(repository).save(config);
    }

    @Test
    void getConfig() {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        when(repository.findByDomain("test.agent")).thenReturn(Optional.of(config));

        Optional<AgentConfig> found = manager.getConfig("test.agent");
        assertTrue(found.isPresent());
        assertEquals("test.agent", found.get().getDomain());
    }

    @Test
    void getAll() {
        AgentConfig config = new AgentConfig();
        when(repository.findByDomainStartingWith("test")).thenReturn(List.of(config));

        List<AgentConfig> found = manager.getAll("test");
        assertEquals(1, found.size());
    }

    @Test
    void deleteConfig() {
        manager.deleteConfig("test.agent");
        verify(repository).deleteByDomain("test.agent");
    }
}
