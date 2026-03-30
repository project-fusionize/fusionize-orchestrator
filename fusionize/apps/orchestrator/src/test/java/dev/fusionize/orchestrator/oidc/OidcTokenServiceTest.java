package dev.fusionize.orchestrator.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcTokenServiceTest {

    @Mock
    private OAuth2AuthorizedClientManager clientManager;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    private OidcTokenService oidcTokenService;

    @BeforeEach
    void setUp() {
        oidcTokenService = new OidcTokenService(clientManager, clientRegistrationRepository);
    }

    @Test
    void shouldGetClientCredentialsToken() {
        // setup
        when(accessToken.getTokenValue()).thenReturn("test-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(clientManager.authorize(any())).thenReturn(authorizedClient);

        // expectation
        String token = oidcTokenService.getClientCredentialsToken();

        // validation
        assertThat(token).isEqualTo("test-token");
    }

    @Test
    void shouldThrowWhenClientIsNull() {
        // setup
        when(clientManager.authorize(any())).thenReturn(null);

        // expectation & validation
        assertThatThrownBy(() -> oidcTokenService.getClientCredentialsToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to obtain access token");
    }

    @Test
    void shouldThrowWhenAccessTokenIsNull() {
        // setup
        when(authorizedClient.getAccessToken()).thenReturn(null);
        when(clientManager.authorize(any())).thenReturn(authorizedClient);

        // expectation & validation
        assertThatThrownBy(() -> oidcTokenService.getClientCredentialsToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to obtain access token");
    }
}
