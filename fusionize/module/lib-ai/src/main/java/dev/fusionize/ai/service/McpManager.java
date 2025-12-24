
package dev.fusionize.ai.service;

import dev.fusionize.ai.model.McpClientConfig;
import dev.fusionize.ai.repo.McpClientConfigRepository;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class McpManager {

    private final McpClientConfigRepository repository;

    public McpManager(McpClientConfigRepository repository) {
        this.repository = repository;
    }

    public McpClientConfig saveConfig(McpClientConfig config) {
        return repository.save(config);
    }

    public Optional<McpClientConfig> getConfig(String key) {
        return repository.findByKey(key);
    }

    public Optional<McpClientConfig> getConfigByDomain(String domain) {
        return repository.findByDomain(domain);
    }

    public McpSyncClient getSyncClient(String key) {
        McpClientConfig config = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("MCP client config not found for key: " + key));

        if (config.getClientType() != McpClientConfig.ClientType.SYNC) {
            throw new IllegalArgumentException("Client type is not SYNC for key: " + key);
        }

        if (config.getTransportType() == McpClientConfig.TransportType.SSE
                || config.getTransportType() == McpClientConfig.TransportType.STREAMABLE_HTTP) {
            McpSyncClient client = McpClient.sync(getMcpClientTransport(config))
                    .requestTimeout(Duration.ofMillis(
                            config.getRequestTimeout() != null
                                    ? config.getRequestTimeout()
                                    : 30000))
                    .build();
            initializeSyncClient(client);
            return client;
        }

        throw new UnsupportedOperationException("Transport type not supported: " + config.getTransportType());
    }

    private McpClientTransport getMcpClientTransport(McpClientConfig config) {
        String fullUrl = config.getUrl();
        if (config.getEndpoint() != null && !config.getEndpoint().isEmpty()) {
            if (!fullUrl.endsWith("/") && !config.getEndpoint().startsWith("/")) {
                fullUrl += "/" + config.getEndpoint();
            } else if (fullUrl.endsWith("/") && config.getEndpoint().startsWith("/")) {
                fullUrl += config.getEndpoint().substring(1);
            } else {
                fullUrl += config.getEndpoint();
            }
        }

        return HttpClientSseClientTransport.builder(fullUrl).build();
    }

    public McpAsyncClient getAsyncClient(String key) {
        McpClientConfig config = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("MCP client config not found for key: " + key));

        if (config.getClientType() != McpClientConfig.ClientType.ASYNC) {
            throw new IllegalArgumentException("Client type is not ASYNC for key: " + key);
        }

        if (config.getTransportType() == McpClientConfig.TransportType.SSE
                || config.getTransportType() == McpClientConfig.TransportType.STREAMABLE_HTTP) {
            McpAsyncClient client = McpClient.async(getMcpClientTransport(config))
                    .requestTimeout(
                            Duration.ofMillis(config.getRequestTimeout() != null ? config.getRequestTimeout() : 30000))
                    .build();
            initializeAsyncClient(client); // Initialize async client
            return client;
        }

        throw new UnsupportedOperationException("Transport type not supported: " + config.getTransportType());
    }

    protected void initializeSyncClient(McpSyncClient client) {
        client.initialize();
    }

    protected void initializeAsyncClient(McpAsyncClient client) {
        client.initialize().block();
    }
}
