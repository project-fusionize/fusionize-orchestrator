package dev.fusionize.ai.service;

import dev.fusionize.ai.model.McpClientConfig;
import dev.fusionize.ai.repo.McpClientConfigRepository;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpManagerTest {

    @Mock
    private McpClientConfigRepository repository;

    private McpManager manager;

    @BeforeEach
    void setUp() {
        manager = spy(new McpManager(repository));
    }

    @Test
    void saveConfig_ShouldSaveAndReturnConfig() {
        McpClientConfig config = new McpClientConfig();
        config.setClientName("test-client");
        when(repository.save(any(McpClientConfig.class))).thenReturn(config);

        McpClientConfig saved = manager.saveConfig(config);

        assertNotNull(saved);
        assertEquals("test-client", saved.getClientName());
        verify(repository).save(config);
    }

    @Test
    void getConfig_ShouldReturnConfig_WhenFound() {
        McpClientConfig config = new McpClientConfig();
        config.setClientName("test-client");
        when(repository.findByKey("test-key")).thenReturn(Optional.of(config));

        Optional<McpClientConfig> result = manager.getConfig("test-key");

        assertTrue(result.isPresent());
        assertEquals("test-client", result.get().getClientName());
    }

    @Test
    void getSyncClient_ShouldThrowException_WhenConfigNotFound() {
        when(repository.findByKey("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> manager.getSyncClient("unknown"));
    }

    @Test
    void getSyncClient_ShouldThrowException_WhenTypeMismatch() {
        McpClientConfig config = new McpClientConfig();
        config.setClientType(McpClientConfig.ClientType.ASYNC);
        when(repository.findByKey("async-key")).thenReturn(Optional.of(config));

        assertThrows(IllegalArgumentException.class, () -> manager.getSyncClient("async-key"));
    }

    @Test
    void getSyncClient_ShouldReturnClient_WhenConfigIsValid_SSE() {
        McpClientConfig config = new McpClientConfig();
        config.setClientType(McpClientConfig.ClientType.SYNC);
        config.setTransportType(McpClientConfig.TransportType.SSE);
        config.setUrl("http://localhost:8080");
        config.setEndpoint("/sse");
        when(repository.findByKey("sync-key")).thenReturn(Optional.of(config));
        doNothing().when(manager).initializeSyncClient(any());

        McpSyncClient client = manager.getSyncClient("sync-key");

        assertNotNull(client);
    }

    @Test
    void getAsyncClient_ShouldReturnClient_WhenConfigIsValid_StreamableHTTP() {
        McpClientConfig config = new McpClientConfig();
        config.setClientType(McpClientConfig.ClientType.ASYNC);
        config.setTransportType(McpClientConfig.TransportType.STREAMABLE_HTTP);
        config.setUrl("http://localhost:8080");
        config.setEndpoint("/mcp");
        when(repository.findByKey("async-key")).thenReturn(Optional.of(config));
        doNothing().when(manager).initializeAsyncClient(any());

        McpAsyncClient client = manager.getAsyncClient("async-key");

        assertNotNull(client);
    }
}
