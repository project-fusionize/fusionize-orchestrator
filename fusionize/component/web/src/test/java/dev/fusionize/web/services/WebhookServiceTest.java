package dev.fusionize.web.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
    }

    @Test
    void testAddAndInvokeListener() {
        WebhookService.WebhookKey key = new WebhookService.WebhookKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);
        Map<String, Object> payload = Map.of("data", "test");

        webhookService.addListener(key, body -> {
            invoked.set(true);
            assertEquals("test", body.get("data"));
        });

        webhookService.invoke(key, payload);

        assertTrue(invoked.get());
    }

    @Test
    void testRemoveListener() {
        WebhookService.WebhookKey key = new WebhookService.WebhookKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);

        webhookService.addListener(key, body -> invoked.set(true));
        webhookService.removeListener(key);
        webhookService.invoke(key, Map.of());

        assertFalse(invoked.get());
    }

    @Test
    void testInvokeUnknownKey() {
        WebhookService.WebhookKey key = new WebhookService.WebhookKey("wf1", "node1");
        // Should not throw exception
        webhookService.invoke(key, Map.of());
    }
}
