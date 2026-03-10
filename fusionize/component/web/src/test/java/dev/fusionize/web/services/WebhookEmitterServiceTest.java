package dev.fusionize.web.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookEmitterServiceTest {

    private WebhookEmitterService service;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any(String[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        service = new WebhookEmitterService(restClient);
    }

    @Test
    void send_deliversSuccessfully() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com/notify", Map.of("event", "done"), null, null);
        var response = service.send(request);

        assertTrue(response.delivered());
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.responseBody());
    }

    @Test
    void send_withSecret_addsSignatureHeader() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", Map.of("event", "done"), "my-secret", null);
        service.send(request);

        verify(requestBodySpec).header(eq("X-Webhook-Signature"), startsWith("sha256="));
    }

    @Test
    void send_withCustomHeaders() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload",
                null, Map.of("X-Api-Key", "key123"));
        service.send(request);

        verify(requestBodySpec).header("X-Api-Key", "key123");
    }

    @Test
    void send_handlesClientError() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));
        when(ex.getResponseBodyAsString()).thenReturn("Bad Request");
        when(responseSpec.toEntity(String.class)).thenThrow(ex);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload", null, null);
        var response = service.send(request);

        assertFalse(response.delivered());
        assertEquals(400, response.statusCode());
        assertEquals("Bad Request", response.responseBody());
    }

    @Test
    void send_handlesServerError() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(503));
        when(ex.getResponseBodyAsString()).thenReturn("Service Unavailable");
        when(responseSpec.toEntity(String.class)).thenThrow(ex);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload", null, null);
        var response = service.send(request);

        assertFalse(response.delivered());
        assertEquals(503, response.statusCode());
    }

    @Test
    void send_handlesConnectionException() {
        when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException("Connection refused"));

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload", null, null);
        var response = service.send(request);

        assertFalse(response.delivered());
        assertEquals(0, response.statusCode());
        assertTrue(response.responseBody().contains("Connection refused"));
    }

    @Test
    void send_withNullPayload() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", null, null, null);
        var response = service.send(request);

        assertTrue(response.delivered());
        verify(requestBodySpec, never()).body(any());
    }

    @Test
    void send_withNullSecret_noSignatureHeader() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload", null, null);
        service.send(request);

        verify(requestBodySpec, never()).header(eq("X-Webhook-Signature"), any(String[].class));
    }

    @Test
    void send_withEmptySecret_noSignatureHeader() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        var request = new WebhookEmitterService.WebhookRequest(
                "https://hooks.example.com", "payload", "", null);
        service.send(request);

        verify(requestBodySpec, never()).header(eq("X-Webhook-Signature"), any(String[].class));
    }

    @Test
    void computeSignature_isConsistent() {
        String sig1 = service.computeSignature("payload", "secret");
        String sig2 = service.computeSignature("payload", "secret");
        assertEquals(sig1, sig2);
    }

    @Test
    void computeSignature_differsForDifferentPayloads() {
        String sig1 = service.computeSignature("payload1", "secret");
        String sig2 = service.computeSignature("payload2", "secret");
        assertNotEquals(sig1, sig2);
    }

    @Test
    void computeSignature_differsForDifferentSecrets() {
        String sig1 = service.computeSignature("payload", "secret1");
        String sig2 = service.computeSignature("payload", "secret2");
        assertNotEquals(sig1, sig2);
    }

    @Test
    void computeSignature_startsWithSha256Prefix() {
        String sig = service.computeSignature("payload", "secret");
        assertTrue(sig.startsWith("sha256="));
    }

    @Test
    void responseToMap_containsAllFields() {
        var response = new WebhookEmitterService.WebhookResponse(true, 200, "OK");
        var map = response.toMap();

        assertEquals(true, map.get("delivered"));
        assertEquals(200, map.get("statusCode"));
        assertEquals("OK", map.get("responseBody"));
    }
}
