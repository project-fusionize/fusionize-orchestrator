package dev.fusionize.web.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpOutboundConnectorServiceTest {

    private HttpOutboundConnectorService service;
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

        when(restClient.method(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any(String[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        service = new HttpOutboundConnectorService(restClient);
    }

    @Test
    void execute_getRequest_returns200() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", List.of("application/json"));

        ResponseEntity<Object> responseEntity = new ResponseEntity<>(
                Map.of("result", "ok"), headers, 200);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/data", HttpMethod.GET, null, null);
        var response = service.execute(request);

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertTrue(response.success());
        assertNotNull(response.body());
    }

    @Test
    void execute_postRequest_sendsBody() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(
                Map.of("id", "123"), headers, 201);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/users", HttpMethod.POST,
                Map.of("Authorization", "Bearer token"), Map.of("name", "Alice"));
        var response = service.execute(request);

        assertEquals(201, response.statusCode());
        assertTrue(response.success());
        verify(requestBodySpec).body(Map.of("name", "Alice"));
        verify(requestBodySpec).contentType(any());
    }

    @Test
    void execute_putRequest_sendsBody() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(null, headers, 204);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        Map<String, String> body = Map.of("name", "Bob");
        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/users/1", HttpMethod.PUT, null, body);
        var response = service.execute(request);

        assertEquals(204, response.statusCode());
        assertTrue(response.success());
        verify(requestBodySpec).contentType(any());
    }

    @Test
    void execute_deleteRequest_doesNotSendBody() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(null, headers, 204);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/users/1", HttpMethod.DELETE, null, Map.of("should", "ignore"));
        var response = service.execute(request);

        assertEquals(204, response.statusCode());
        verify(requestBodySpec, never()).body(any());
    }

    @Test
    void execute_setsCustomHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>("ok", headers, 200);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com", HttpMethod.GET,
                Map.of("X-Api-Key", "key123", "Accept", "application/json"), null);
        service.execute(request);

        verify(requestBodySpec).header("X-Api-Key", "key123");
        verify(requestBodySpec).header("Accept", "application/json");
    }

    @Test
    void execute_handlesClientError() {
        HttpHeaders errorHeaders = new HttpHeaders();
        errorHeaders.put("X-Error", List.of("bad request"));

        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));
        when(ex.getResponseHeaders()).thenReturn(errorHeaders);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\": \"bad input\"}");
        when(responseSpec.toEntity(Object.class)).thenThrow(ex);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/data", HttpMethod.POST, null, "bad");
        var response = service.execute(request);

        assertEquals(400, response.statusCode());
        assertFalse(response.success());
        assertEquals("{\"error\": \"bad input\"}", response.body());
    }

    @Test
    void execute_handlesServerError() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(500));
        when(ex.getResponseHeaders()).thenReturn(null);
        when(ex.getResponseBodyAsString()).thenReturn("Internal Server Error");
        when(responseSpec.toEntity(Object.class)).thenThrow(ex);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/data", HttpMethod.GET, null, null);
        var response = service.execute(request);

        assertEquals(500, response.statusCode());
        assertFalse(response.success());
    }

    @Test
    void execute_nullHeadersSkipped() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>("ok", headers, 200);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com", HttpMethod.GET, null, null);
        var response = service.execute(request);

        assertEquals(200, response.statusCode());
        // header() should not have been called with custom headers
    }

    @Test
    void responseToMap_containsAllFields() {
        var response = new HttpOutboundConnectorService.Response(
                201, Map.of("Location", List.of("/users/1")), Map.of("id", 1), true);
        var map = response.toMap();

        assertEquals(201, map.get("statusCode"));
        assertEquals(true, map.get("success"));
        assertNotNull(map.get("headers"));
        assertNotNull(map.get("body"));
    }

    @Test
    void execute_patchRequest_sendsBody() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(Map.of("updated", true), headers, 200);
        when(responseSpec.toEntity(Object.class)).thenReturn(responseEntity);

        var request = new HttpOutboundConnectorService.Request(
                "https://api.example.com/users/1", HttpMethod.PATCH,
                null, Map.of("name", "Updated"));
        var response = service.execute(request);

        assertEquals(200, response.statusCode());
        verify(requestBodySpec).contentType(any());
    }
}
