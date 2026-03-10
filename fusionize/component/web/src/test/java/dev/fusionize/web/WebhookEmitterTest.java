package dev.fusionize.web;

import dev.fusionize.web.services.WebhookEmitterService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookEmitterTest {

    private WebhookEmitter emitterComponent;
    private WebhookEmitterService webhookService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        webhookService = mock(WebhookEmitterService.class);
        emitterComponent = new WebhookEmitter(webhookService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure ---

    @Test
    void configure_setsUrlAndDefaults() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com/notify");
        emitterComponent.configure(config);

        context.set("data", Map.of("event", "completed"));
        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        config.set("input", "payload");
        config.set("output", "result");
        emitterComponent.configure(config);

        context.set("payload", Map.of("msg", "hello"));

        when(webhookService.send(any())).thenReturn(
                new WebhookEmitterService.WebhookResponse(true, 200, "ok"));

        emitterComponent.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("result"));
    }

    @Test
    void configure_setsSecretAndHeaders() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        config.set("secret", "my-secret-key");
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Custom", "value");
        config.set("headers", headers);
        emitterComponent.configure(config);

        context.set("data", "content");
        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- canActivate ---

    @Test
    void canActivate_failsWhenUrlMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        emitterComponent.configure(config);

        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("URL"));
    }

    @Test
    void canActivate_failsWhenUrlEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "");
        emitterComponent.configure(config);

        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        emitterComponent.configure(config);

        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("data"));
    }

    @Test
    void canActivate_succeedsWithAllConfig() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        emitterComponent.configure(config);

        context.set("data", Map.of("event", "done"));
        emitterComponent.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run ---

    @Test
    void run_sendsWebhookSuccessfully() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com/notify");
        emitterComponent.configure(config);

        context.set("data", Map.of("event", "workflow.completed", "id", "exec-123"));

        WebhookEmitterService.WebhookResponse mockResponse =
                new WebhookEmitterService.WebhookResponse(true, 200, "OK");
        when(webhookService.send(any())).thenReturn(mockResponse);

        emitterComponent.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("webhookResult");
        assertNotNull(result);
        assertEquals(true, result.get("delivered"));
        assertEquals(200, result.get("statusCode"));
    }

    @Test
    void run_handlesDeliveryFailure() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        emitterComponent.configure(config);

        context.set("data", Map.of("event", "test"));

        WebhookEmitterService.WebhookResponse mockResponse =
                new WebhookEmitterService.WebhookResponse(false, 503, "Service Unavailable");
        when(webhookService.send(any())).thenReturn(mockResponse);

        emitterComponent.run(context, emitter);

        // Component succeeds — the webhook delivery status is in the output
        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("webhookResult");
        assertEquals(false, result.get("delivered"));
        assertEquals(503, result.get("statusCode"));
    }

    @Test
    void run_resolvesUrlPlaceholders() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com/{tenant}/notify");
        emitterComponent.configure(config);

        context.set("data", Map.of("event", "test"));
        context.set("tenant", "acme");

        WebhookEmitterService.WebhookResponse mockResponse =
                new WebhookEmitterService.WebhookResponse(true, 200, "ok");
        when(webhookService.send(any())).thenReturn(mockResponse);

        emitterComponent.run(context, emitter);

        verify(webhookService).send(argThat(req ->
                req.url().equals("https://hooks.example.com/acme/notify")));
    }

    @Test
    void run_passesSecretToService() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        config.set("secret", "webhook-secret-123");
        emitterComponent.configure(config);

        context.set("data", "payload");

        WebhookEmitterService.WebhookResponse mockResponse =
                new WebhookEmitterService.WebhookResponse(true, 200, "ok");
        when(webhookService.send(any())).thenReturn(mockResponse);

        emitterComponent.run(context, emitter);

        verify(webhookService).send(argThat(req ->
                "webhook-secret-123".equals(req.secret())));
    }

    @Test
    void run_passesHeadersToService() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Api-Key", "key123");
        config.set("headers", headers);
        emitterComponent.configure(config);

        context.set("data", "payload");

        WebhookEmitterService.WebhookResponse mockResponse =
                new WebhookEmitterService.WebhookResponse(true, 200, "ok");
        when(webhookService.send(any())).thenReturn(mockResponse);

        emitterComponent.run(context, emitter);

        verify(webhookService).send(argThat(req ->
                req.headers() != null && "key123".equals(req.headers().get("X-Api-Key"))));
    }

    @Test
    void run_handlesServiceException() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("url", "https://hooks.example.com");
        emitterComponent.configure(config);

        context.set("data", "payload");
        when(webhookService.send(any())).thenThrow(new RuntimeException("Network error"));

        emitterComponent.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Network error"));
    }
}
