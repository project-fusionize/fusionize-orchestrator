package dev.fusionize.worker.oidc;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

public class OidcTokenClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OidcTokenClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get client credentials token with custom parameters
     *
     * @param clientId Client ID (null to use default)
     * @param clientSecret Client Secret (null to use default)
     * @param tokenUri Token URI (null to use default)
     * @param scope Scope (null to use default)
     * @return Access token
     */
    public String getClientCredentialsToken(String clientId, String clientSecret,
                                            String tokenUri, String scope) throws OidcTokenClientException {
        try {
            HttpHeaders headers = new HttpHeaders();

            // Add Basic Auth if credentials provided
            if (clientId != null && clientSecret != null) {
                String auth = clientId + ":" + clientSecret;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
            }

            // Add custom headers
            if (tokenUri != null) {
                headers.set("X-Token-Uri", tokenUri);
            }
            if (scope != null) {
                headers.set("X-Scope", scope);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/auth/token/client",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            throw new OidcTokenClientException("(oidc101) Failed to obtain client credentials token");

        } catch (HttpClientErrorException e) {
            throw new OidcTokenClientException("(oidc101) Error obtaining token: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new OidcTokenClientException("(oidc101) Error obtaining token: " + e.getMessage(), e);
        }
    }


}
