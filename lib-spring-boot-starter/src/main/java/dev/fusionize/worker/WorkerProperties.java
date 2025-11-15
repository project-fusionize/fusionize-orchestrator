package dev.fusionize.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;

@ConfigurationProperties(prefix = "fusionize.worker")
public class WorkerProperties {
    private String orchestratorUrl;
    private String orchestratorMongo;
    private String oidcClientId;
    private String oidcClientSecret;
    private String workflowDefinitionsRoot = "workflows";

    public String getOrchestratorUrl() {
        return orchestratorUrl;
    }

    public void setOrchestratorUrl(String orchestratorUrl) {
        this.orchestratorUrl = orchestratorUrl;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public String getOrchestratorMongo() {
        return orchestratorMongo;
    }

    public void setOrchestratorMongo(String orchestratorMongo) {
        this.orchestratorMongo = orchestratorMongo;
    }

    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }

    public String getWorkflowDefinitionsRoot() {
        return workflowDefinitionsRoot;
    }

    public void setWorkflowDefinitionsRoot(String workflowDefinitionsRoot) {
        this.workflowDefinitionsRoot = workflowDefinitionsRoot;
    }

    public static String toWebSocketUrl(String httpUrl) {
        if (httpUrl == null || httpUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            URI uri = new URI(httpUrl.trim());
            String scheme = uri.getScheme();

            if (scheme == null) {
                throw new IllegalArgumentException("URL must have a scheme (http:// or https://)");
            }

            String wsScheme;
            switch (scheme.toLowerCase()) {
                case "http":
                    wsScheme = "ws";
                    break;
                case "https":
                    wsScheme = "wss";
                    break;
                case "ws":
                case "wss":
                    // Already a WebSocket URL, return as-is
                    return httpUrl;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported scheme: " + scheme + ". Expected http or https");
            }

            // Rebuild URI with WebSocket scheme
            return new URI(
                    wsScheme,
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + httpUrl, e);
        }
    }
}
