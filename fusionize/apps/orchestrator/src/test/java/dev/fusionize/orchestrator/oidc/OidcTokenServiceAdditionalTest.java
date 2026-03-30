package dev.fusionize.orchestrator.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcTokenServiceAdditionalTest {

    @Mock
    private OAuth2AuthorizedClientManager clientManager;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @Mock
    private RestTemplate restTemplate;

    private OidcTokenService oidcTokenService;

    @BeforeEach
    void setUp() throws Exception {
        // setup
        oidcTokenService = new OidcTokenService(clientManager, clientRegistrationRepository);

        // inject mock RestTemplate via reflection
        Field restTemplateField = OidcTokenService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(oidcTokenService, restTemplate);
    }

    @Test
    void shouldGetStaticClientCredentialsToken() {
        // setup
        when(accessToken.getTokenValue()).thenReturn("static-cc-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(clientManager.authorize(any())).thenReturn(authorizedClient);

        // expectation
        String token = oidcTokenService.getClientCredentialsToken();

        // validation
        assertThat(token).isEqualTo("static-cc-token");
        verify(clientManager).authorize(any());
    }

    @Test
    void shouldDelegateToStaticFlow_whenNoCustomParams() {
        // setup
        when(accessToken.getTokenValue()).thenReturn("delegated-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(clientManager.authorize(any())).thenReturn(authorizedClient);

        ClientRegistration configuredClient = ClientRegistration
                .withRegistrationId("fusionize-client-credentials")
                .clientId("configured-client")
                .clientSecret("configured-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost/token")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-client-credentials"))
                .thenReturn(configuredClient);

        // expectation
        String token = oidcTokenService.getClientCredentialsToken(null, null, null, null);

        // validation
        assertThat(token).isEqualTo("delegated-token");
        verify(clientManager).authorize(any());
    }

    @Test
    void shouldThrow_whenClientIsNull_forStaticFlow() {
        // setup
        when(clientManager.authorize(any())).thenReturn(null);

        // expectation & validation
        assertThatThrownBy(() -> oidcTokenService.getClientCredentialsToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to obtain access token");
    }

    @Test
    void shouldGetPasswordToken_delegatesToFullMethod() {
        // setup
        ClientRegistration passwordClient = ClientRegistration
                .withRegistrationId("fusionize-password")
                .clientId("password-client")
                .clientSecret("password-secret")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .tokenUri("http://localhost/token")
                .scope("openid")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-password"))
                .thenReturn(passwordClient);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = new ResponseEntity<>(
                Map.of("access_token", "password-token"), HttpStatus.OK);
        when(restTemplate.postForEntity(eq("http://localhost/token"), any(), eq(Map.class)))
                .thenReturn(response);

        // expectation
        String token = oidcTokenService.getPasswordToken("user1", "pass1");

        // validation
        assertThat(token).isEqualTo("password-token");
        verify(clientRegistrationRepository).findByRegistrationId("fusionize-password");
        verify(restTemplate).postForEntity(eq("http://localhost/token"), any(), eq(Map.class));
    }

    @Test
    void shouldGetPasswordToken_withCustomParams() {
        // setup
        ClientRegistration passwordClient = ClientRegistration
                .withRegistrationId("fusionize-password")
                .clientId("default-client")
                .clientSecret("default-secret")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .tokenUri("http://default/token")
                .scope("openid")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-password"))
                .thenReturn(passwordClient);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = new ResponseEntity<>(
                Map.of("access_token", "custom-password-token"), HttpStatus.OK);
        when(restTemplate.postForEntity(eq("http://custom/token"), any(), eq(Map.class)))
                .thenReturn(response);

        // expectation
        String token = oidcTokenService.getPasswordToken(
                "user1", "pass1", "custom-client", "custom-secret", "http://custom/token", "custom-scope");

        // validation
        assertThat(token).isEqualTo("custom-password-token");
        verify(restTemplate).postForEntity(eq("http://custom/token"), any(), eq(Map.class));
    }

    @Test
    void shouldThrow_whenPasswordTokenResponseIsNotOk() {
        // setup
        ClientRegistration passwordClient = ClientRegistration
                .withRegistrationId("fusionize-password")
                .clientId("password-client")
                .clientSecret("password-secret")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .tokenUri("http://localhost/token")
                .scope("openid")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-password"))
                .thenReturn(passwordClient);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        when(restTemplate.postForEntity(eq("http://localhost/token"), any(), eq(Map.class)))
                .thenReturn(response);

        // expectation & validation
        assertThatThrownBy(() -> oidcTokenService.getPasswordToken("user1", "pass1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Error obtaining password grant token");
    }

    @Test
    void shouldThrow_whenRestTemplateThrowsException() {
        // setup
        ClientRegistration passwordClient = ClientRegistration
                .withRegistrationId("fusionize-password")
                .clientId("password-client")
                .clientSecret("password-secret")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .tokenUri("http://localhost/token")
                .scope("openid")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-password"))
                .thenReturn(passwordClient);

        when(restTemplate.postForEntity(eq("http://localhost/token"), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // expectation & validation
        assertThatThrownBy(() -> oidcTokenService.getPasswordToken("user1", "pass1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Error obtaining password grant token")
                .hasMessageContaining("Connection refused");
    }

    @Test
    void shouldGetPasswordToken_withEmptyScope() {
        // setup
        ClientRegistration passwordClient = ClientRegistration
                .withRegistrationId("fusionize-password")
                .clientId("password-client")
                .clientSecret("password-secret")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .tokenUri("http://localhost/token")
                .scope("")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("fusionize-password"))
                .thenReturn(passwordClient);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = new ResponseEntity<>(
                Map.of("access_token", "empty-scope-token"), HttpStatus.OK);
        when(restTemplate.postForEntity(eq("http://localhost/token"), any(), eq(Map.class)))
                .thenReturn(response);

        // expectation
        String token = oidcTokenService.getPasswordToken("user1", "pass1");

        // validation
        assertThat(token).isEqualTo("empty-scope-token");
    }
}
