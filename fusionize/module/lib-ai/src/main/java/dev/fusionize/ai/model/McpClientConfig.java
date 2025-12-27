package dev.fusionize.ai.model;

import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ai_mcp_client_config")
public class McpClientConfig extends DomainEntity {

    public enum ClientType {
        SYNC, ASYNC
    }

    public enum TransportType {
        SSE, STREAMABLE_HTTP
    }

    private String clientName;
    private String clientVersion;
    private Long requestTimeout; // in milliseconds
    private ClientType clientType = ClientType.SYNC;
    private TransportType transportType = TransportType.SSE;

    // Common
    private boolean rootChangeNotification = true;
    private boolean toolCallbackEnabled = true;

    // HTTP / SSE / Streamable HTTP
    private String url;
    private String endpoint;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public Long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isRootChangeNotification() {
        return rootChangeNotification;
    }

    public void setRootChangeNotification(boolean rootChangeNotification) {
        this.rootChangeNotification = rootChangeNotification;
    }

    public boolean isToolCallbackEnabled() {
        return toolCallbackEnabled;
    }

    public void setToolCallbackEnabled(boolean toolCallbackEnabled) {
        this.toolCallbackEnabled = toolCallbackEnabled;
    }

    public static Builder builder(String parentDomain) {
        return new Builder(parentDomain);
    }

    public static class Builder extends DomainEntity.Builder<Builder> {
        private String clientName;
        private String clientVersion;
        private Long requestTimeout;
        private ClientType clientType = ClientType.SYNC;
        private TransportType transportType = TransportType.SSE;
        private boolean rootChangeNotification = true;
        private boolean toolCallbackEnabled = true;
        private String url;
        private String endpoint;

        private Builder(String parentDomain) {
            super(parentDomain);
        }

        public Builder withClientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder withClientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder withRequestTimeout(Long requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder withClientType(ClientType clientType) {
            this.clientType = clientType;
            return this;
        }

        public Builder withTransportType(TransportType transportType) {
            this.transportType = transportType;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder withRootChangeNotification(boolean rootChangeNotification) {
            this.rootChangeNotification = rootChangeNotification;
            return this;
        }

        public Builder withToolCallbackEnabled(boolean toolCallbackEnabled) {
            this.toolCallbackEnabled = toolCallbackEnabled;
            return this;
        }

        @Override
        public McpClientConfig build() {
            McpClientConfig config = new McpClientConfig();
            config.load(super.build());
            config.setClientName(this.clientName);
            config.setClientVersion(this.clientVersion);
            config.setRequestTimeout(this.requestTimeout);
            config.setClientType(this.clientType);
            config.setTransportType(this.transportType);
            config.setUrl(this.url);
            config.setEndpoint(this.endpoint);
            config.setRootChangeNotification(this.rootChangeNotification);
            config.setToolCallbackEnabled(this.toolCallbackEnabled);
            return config;
        }
    }
}
