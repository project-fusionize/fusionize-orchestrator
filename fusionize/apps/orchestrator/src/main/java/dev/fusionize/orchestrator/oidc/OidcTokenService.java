package dev.fusionize.orchestrator.oidc;

import org.springframework.http.*;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class OidcTokenService {

    private final OAuth2AuthorizedClientManager clientManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;

    public OidcTokenService(OAuth2AuthorizedClientManager clientManager,
                            ClientRegistrationRepository clientRegistrationRepository) {
        this.clientManager = clientManager;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Dynamic Client Credentials Token - uses provided params or falls back to config
     */
    public String getClientCredentialsToken(String clientId, String clientSecret,
                                            String tokenUri, String scope) {

        // Get configured client registration as base
        ClientRegistration configuredClient = clientRegistrationRepository
                .findByRegistrationId("fusionize-client-credentials");

        // If no custom params provided, use the configured client directly
        if (!hasCustomParams(clientId, clientSecret, tokenUri, scope)) {
            return getClientCredentialsToken();
        }

        // Build new registration based on configured one, overriding with custom params
        ClientRegistration.Builder builder = ClientRegistration
                .withClientRegistration(configuredClient);

        if (StringUtils.hasText(clientId)) {
            builder.registrationId(clientId).clientId(clientId);
        }
        if (StringUtils.hasText(clientSecret)) {
            builder.clientSecret(clientSecret);
        }
        if (StringUtils.hasText(tokenUri)) {
            builder.tokenUri(tokenUri);
        }
        if (StringUtils.hasText(scope)) {
            builder.scope(scope.split("\\s+"));
        }

        ClientRegistration registration = builder.build();

        InMemoryClientRegistrationRepository registrationRepo =
                new InMemoryClientRegistrationRepository(registration);

        InMemoryOAuth2AuthorizedClientService clientService =
                new InMemoryOAuth2AuthorizedClientService(registrationRepo);

        AuthorizedClientServiceOAuth2AuthorizedClientManager dynamicManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrationRepo, clientService);

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(registration.getRegistrationId())
                .principal(registration.getClientId())
                .build();

        OAuth2AuthorizedClient client = dynamicManager.authorize(request);

        if (client != null && client.getAccessToken() != null) {
            return client.getAccessToken().getTokenValue();
        }

        throw new IllegalStateException("Failed to obtain token for client: " + registration.getClientId());
    }

    /**
     * Password Grant Token - uses provided params or falls back to config
     */
    public String getPasswordToken(String username, String password,
                                   String clientId, String clientSecret,
                                   String tokenUri, String scope) {

        // Get configured client registration for defaults
        ClientRegistration configuredClient = clientRegistrationRepository
                .findByRegistrationId("fusionize-password");

        // Use configured values as defaults
        String effectiveClientId = StringUtils.hasText(clientId)
                ? clientId : configuredClient.getClientId();
        String effectiveClientSecret = StringUtils.hasText(clientSecret)
                ? clientSecret : configuredClient.getClientSecret();
        String effectiveTokenUri = StringUtils.hasText(tokenUri)
                ? tokenUri : configuredClient.getProviderDetails().getTokenUri();
        String effectiveScope = StringUtils.hasText(scope)
                ? scope : String.join(" ", configuredClient.getScopes());

        try {
            // Prepare request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add(OAuth2ParameterNames.GRANT_TYPE, "password");
            requestBody.add(OAuth2ParameterNames.USERNAME, username);
            requestBody.add(OAuth2ParameterNames.PASSWORD, password);
            if (StringUtils.hasText(effectiveScope)) {
                requestBody.add(OAuth2ParameterNames.SCOPE, effectiveScope);
            }

            // Prepare headers with Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = effectiveClientId + ":" + effectiveClientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            // Make request
            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    effectiveTokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            throw new IllegalStateException("Failed to obtain password grant token");

        } catch (Exception e) {
            throw new IllegalStateException("Error obtaining password grant token: " + e.getMessage(), e);
        }
    }

    /**
     * Static Client Credentials Flow (using pre-configured client)
     */
    public String getClientCredentialsToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("fusionize-client-credentials")
                .principal("fusionize-client")
                .build();
        return getToken(request);
    }

    /**
     * Static Password Grant Flow (using pre-configured client)
     */
    public String getPasswordToken(String username, String password) {
        return getPasswordToken(username, password, null, null, null, null);
    }

    private String getToken(OAuth2AuthorizeRequest request) {
        OAuth2AuthorizedClient client = clientManager.authorize(request);
        if (client != null && client.getAccessToken() != null) {
            return client.getAccessToken().getTokenValue();
        }
        throw new IllegalStateException("Failed to obtain access token");
    }

    private boolean hasCustomParams(String clientId, String clientSecret,
                                    String tokenUri, String scope) {
        return StringUtils.hasText(clientId) ||
                StringUtils.hasText(clientSecret) ||
                StringUtils.hasText(tokenUri) ||
                StringUtils.hasText(scope);
    }
}