package dev.fusionize.orchestrator.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcControllerTest {

    @Mock
    private OidcTokenService tokenService;

    private OidcController controller;

    @BeforeEach
    void setUp() {
        controller = new OidcController(tokenService);
    }

    @Test
    void shouldReturnClientToken_withValidAuth() {
        // setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("client-id:client-secret".getBytes()));
        String tokenUri = "https://auth.example.com/token";
        String scope = "openid";

        // expectation
        when(tokenService.getClientCredentialsToken("client-id", "client-secret", tokenUri, scope))
                .thenReturn("token-123");

        ResponseEntity<Map<String, Object>> response = controller.clientToken(request, tokenUri, scope);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("access_token")).isEqualTo("token-123");
        assertThat(response.getBody().get("token_uri")).isEqualTo(tokenUri);
        assertThat(response.getBody().get("scope")).isEqualTo(scope);
    }

    @Test
    void shouldReturnUnauthorized_whenNoAuthHeader() {
        // setup
        MockHttpServletRequest request = new MockHttpServletRequest();

        // expectation
        ResponseEntity<Map<String, Object>> response = controller.clientToken(request, null, null);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Authorization header is required");
    }

    @Test
    void shouldReturnUnauthorized_whenServiceThrows() {
        // setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("client-id:client-secret".getBytes()));
        String tokenUri = "https://auth.example.com/token";
        String scope = "openid";

        // expectation
        when(tokenService.getClientCredentialsToken("client-id", "client-secret", tokenUri, scope))
                .thenThrow(new RuntimeException("token service unavailable"));

        ResponseEntity<Map<String, Object>> response = controller.clientToken(request, tokenUri, scope);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Invalid authorization");
        assertThat(response.getBody().get("message")).isEqualTo("token service unavailable");
    }

    @Test
    void shouldReturnPasswordToken_withValidAuth() {
        // setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes()));

        // expectation
        when(tokenService.getPasswordToken("user", "pass"))
                .thenReturn("pwd-token");

        ResponseEntity<Map<String, Object>> response = controller.passwordToken(request);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("access_token")).isEqualTo("pwd-token");
    }

    @Test
    void shouldReturnUnauthorized_forPasswordToken_whenNoAuth() {
        // setup
        MockHttpServletRequest request = new MockHttpServletRequest();

        // expectation
        ResponseEntity<Map<String, Object>> response = controller.passwordToken(request);

        // validation
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Authorization header is required");
    }
}
