package dev.fusionize.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientConfigTest {

    @Test
    void shouldBuildWithAllFields() {
        // setup
        var builder = McpClientConfig.builder("parent-domain")
                .withClientName("test-client")
                .withClientVersion("1.0.0")
                .withRequestTimeout(5000L)
                .withClientType(McpClientConfig.ClientType.ASYNC)
                .withTransportType(McpClientConfig.TransportType.STREAMABLE_HTTP)
                .withUrl("http://localhost:8080")
                .withEndpoint("/mcp")
                .withRootChangeNotification(false)
                .withToolCallbackEnabled(false);

        // expectation
        var config = builder.build();

        // validation
        assertThat(config.getClientName()).isEqualTo("test-client");
        assertThat(config.getClientVersion()).isEqualTo("1.0.0");
        assertThat(config.getRequestTimeout()).isEqualTo(5000L);
        assertThat(config.getClientType()).isEqualTo(McpClientConfig.ClientType.ASYNC);
        assertThat(config.getTransportType()).isEqualTo(McpClientConfig.TransportType.STREAMABLE_HTTP);
        assertThat(config.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.getEndpoint()).isEqualTo("/mcp");
        assertThat(config.isRootChangeNotification()).isFalse();
        assertThat(config.isToolCallbackEnabled()).isFalse();
    }

    @Test
    void shouldDefaultClientTypeToSync() {
        // setup / expectation
        var config = McpClientConfig.builder("parent-domain")
                .withClientName("default-client")
                .build();

        // validation
        assertThat(config.getClientType()).isEqualTo(McpClientConfig.ClientType.SYNC);
    }

    @Test
    void shouldDefaultTransportTypeToSse() {
        // setup / expectation
        var config = McpClientConfig.builder("parent-domain")
                .withClientName("default-client")
                .build();

        // validation
        assertThat(config.getTransportType()).isEqualTo(McpClientConfig.TransportType.SSE);
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var config = new McpClientConfig();

        // expectation
        config.setClientName("my-client");
        config.setClientVersion("2.0.0");
        config.setRequestTimeout(10000L);
        config.setClientType(McpClientConfig.ClientType.ASYNC);
        config.setTransportType(McpClientConfig.TransportType.STREAMABLE_HTTP);
        config.setUrl("http://example.com");
        config.setEndpoint("/api");
        config.setRootChangeNotification(false);
        config.setToolCallbackEnabled(false);

        // validation
        assertThat(config.getClientName()).isEqualTo("my-client");
        assertThat(config.getClientVersion()).isEqualTo("2.0.0");
        assertThat(config.getRequestTimeout()).isEqualTo(10000L);
        assertThat(config.getClientType()).isEqualTo(McpClientConfig.ClientType.ASYNC);
        assertThat(config.getTransportType()).isEqualTo(McpClientConfig.TransportType.STREAMABLE_HTTP);
        assertThat(config.getUrl()).isEqualTo("http://example.com");
        assertThat(config.getEndpoint()).isEqualTo("/api");
        assertThat(config.isRootChangeNotification()).isFalse();
        assertThat(config.isToolCallbackEnabled()).isFalse();
    }

    @Test
    void shouldBuildWithName() {
        // setup / expectation
        var config = McpClientConfig.builder("org-domain")
                .withName("My MCP Client")
                .withClientName("mcp-client")
                .build();

        // validation
        assertThat(config.getName()).isEqualTo("My MCP Client");
        assertThat(config.getDomain()).contains("org-domain");
        assertThat(config.getClientName()).isEqualTo("mcp-client");
    }
}
