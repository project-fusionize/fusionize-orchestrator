package dev.fusionize.web;

import dev.fusionize.web.services.HttpOutboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpOutboundConnectorTest {

    private HttpOutboundConnector connector;
    private HttpOutboundConnectorService httpService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpOutboundConnectorService.class);
        connector = new HttpOutboundConnector(httpService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure ---

    @Test
    void configure_setsUrlAndMethod() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        config.set("method", "POST");
        connector.configure(config);

        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_defaultsToGet() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        connector.configure(config);

        when(httpService.execute(any())).thenReturn(
                new HttpOutboundConnectorService.Response(200, Map.of(), "ok", true));

        connector.run(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_setsHeaders() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com");
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Accept", "application/json");
        config.set("headers", headers);
        connector.configure(config);

        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_setsInputAndOutputVars() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com");
        config.set("input", "requestBody");
        config.set("output", "apiResult");
        connector.configure(config);

        context.set("requestBody", Map.of("key", "value"));

        when(httpService.execute(any())).thenReturn(
                new HttpOutboundConnectorService.Response(200, Map.of(), Map.of("r", "v"), true));

        connector.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("apiResult"));
    }

    @Test
    void configure_methodCaseInsensitive() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com");
        config.set("method", "post");
        connector.configure(config);

        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    // --- canActivate ---

    @Test
    void canActivate_failsWhenUrlMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("URL"));
    }

    @Test
    void canActivate_failsWhenUrlEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "");
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenInputVarSetButMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com");
        config.set("input", "requestBody");
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("requestBody"));
    }

    @Test
    void canActivate_succeedsWithoutInputVar() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWithInputVarPresent() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com");
        config.set("input", "body");
        connector.configure(config);

        context.set("body", Map.of("data", "value"));
        connector.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run ---

    @Test
    void run_executesGetRequest() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/users");
        connector.configure(config);

        HttpOutboundConnectorService.Response mockResponse = new HttpOutboundConnectorService.Response(
                200, Map.of("Content-Type", List.of("application/json")),
                Map.of("users", List.of("Alice", "Bob")), true);
        when(httpService.execute(any())).thenReturn(mockResponse);

        connector.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("httpResponse");
        assertNotNull(result);
        assertEquals(200, result.get("statusCode"));
        assertEquals(true, result.get("success"));
    }

    @Test
    void run_executesPostWithBody() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/users");
        config.set("method", "POST");
        config.set("input", "newUser");
        connector.configure(config);

        context.set("newUser", Map.of("name", "Alice", "email", "alice@test.com"));

        HttpOutboundConnectorService.Response mockResponse = new HttpOutboundConnectorService.Response(
                201, Map.of(), Map.of("id", "123"), true);
        when(httpService.execute(any())).thenReturn(mockResponse);

        connector.run(context, emitter);

        assertTrue(emitter.successCalled);
        verify(httpService).execute(argThat(req ->
                req.body() != null && req.url().equals("https://api.example.com/users")));
    }

    @Test
    void run_resolvesUrlPlaceholders() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/users/{userId}/orders");
        connector.configure(config);

        context.set("userId", "42");

        HttpOutboundConnectorService.Response mockResponse = new HttpOutboundConnectorService.Response(
                200, Map.of(), List.of(), true);
        when(httpService.execute(any())).thenReturn(mockResponse);

        connector.run(context, emitter);

        verify(httpService).execute(argThat(req ->
                req.url().equals("https://api.example.com/users/42/orders")));
    }

    @Test
    void run_handlesErrorResponse() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        connector.configure(config);

        HttpOutboundConnectorService.Response mockResponse = new HttpOutboundConnectorService.Response(
                500, Map.of(), "Internal Server Error", false);
        when(httpService.execute(any())).thenReturn(mockResponse);

        connector.run(context, emitter);

        // Still succeeds — the component executed, the response just indicates server error
        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("httpResponse");
        assertEquals(500, result.get("statusCode"));
        assertEquals(false, result.get("success"));
    }

    @Test
    void run_handlesServiceException() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        connector.configure(config);

        when(httpService.execute(any())).thenThrow(new RuntimeException("Connection refused"));

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Connection refused"));
    }

    @Test
    void run_passesNullBodyForGetRequest() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://api.example.com/data");
        connector.configure(config);

        HttpOutboundConnectorService.Response mockResponse = new HttpOutboundConnectorService.Response(
                200, Map.of(), "ok", true);
        when(httpService.execute(any())).thenReturn(mockResponse);

        connector.run(context, emitter);

        verify(httpService).execute(argThat(req -> req.body() == null));
    }
}
