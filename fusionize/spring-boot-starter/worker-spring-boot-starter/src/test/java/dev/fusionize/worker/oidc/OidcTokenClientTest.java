package dev.fusionize.worker.oidc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcTokenClientTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private OidcTokenClient createClientWithMock(String baseUrl) throws Exception {
        var client = new OidcTokenClient(baseUrl);
        Field field = OidcTokenClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(client, mockRestTemplate);
        return client;
    }

    @Test
    void shouldRemoveTrailingSlashFromBaseUrl() throws Exception {
        // setup
        var client = new OidcTokenClient("http://localhost:9999/");

        // expectation
        Field field = OidcTokenClient.class.getDeclaredField("baseUrl");
        field.setAccessible(true);
        var baseUrl = (String) field.get(client);

        // validation
        assertThat(baseUrl).isEqualTo("http://localhost:9999");
    }

    @Test
    void shouldKeepBaseUrlWithoutTrailingSlash() throws Exception {
        // setup
        var client = new OidcTokenClient("http://localhost:9999");

        // expectation
        Field field = OidcTokenClient.class.getDeclaredField("baseUrl");
        field.setAccessible(true);
        var baseUrl = (String) field.get(client);

        // validation
        assertThat(baseUrl).isEqualTo("http://localhost:9999");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnToken_whenResponseIsOk() throws Exception {
        // setup
        var client = createClientWithMock("http://localhost:9999");
        var responseBody = Map.of("access_token", "my-token-123");
        var response = new ResponseEntity<Map>(responseBody, HttpStatus.OK);
        when(mockRestTemplate.exchange(
                eq("http://localhost:9999/auth/token/client"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        // expectation
        var token = client.getClientCredentialsToken("clientId", "clientSecret", "http://token-uri", "openid");

        // validation
        assertThat(token).isEqualTo("my-token-123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowException_whenResponseHasNoBody() throws Exception {
        // setup
        var client = createClientWithMock("http://localhost:9999");
        var response = new ResponseEntity<Map>((Map) null, HttpStatus.OK);
        when(mockRestTemplate.exchange(
                eq("http://localhost:9999/auth/token/client"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        // expectation + validation
        assertThatThrownBy(() -> client.getClientCredentialsToken("clientId", "clientSecret", null, null))
                .isInstanceOf(OidcTokenClientException.class)
                .hasMessageContaining("oidc101");
    }

    @Test
    void shouldThrowException_whenHttpClientError() throws Exception {
        // setup
        var client = createClientWithMock("http://localhost:9999");
        when(mockRestTemplate.exchange(
                eq("http://localhost:9999/auth/token/client"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // expectation + validation
        assertThatThrownBy(() -> client.getClientCredentialsToken("clientId", "clientSecret", null, null))
                .isInstanceOf(OidcTokenClientException.class)
                .hasMessageContaining("oidc101")
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }
}
