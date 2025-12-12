package dev.fusionize.web.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class HttpInboundConnectorServiceTest {

    private HttpInboundConnectorService httpInboundConnectorService;

    @BeforeEach
    void setUp() {
        httpInboundConnectorService = new HttpInboundConnectorService();
    }

    @Test
    void testAddAndInvokeListener() {
        HttpInboundConnectorService.HttpConnectorKey key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);
        Map<String, Object> payload = Map.of("data", "test");

        httpInboundConnectorService.addListener(key, body -> {
            invoked.set(true);
            assertEquals("test", body.get("data"));
        });

        httpInboundConnectorService.invoke(key, payload);

        assertTrue(invoked.get());
    }

    @Test
    void testRemoveListener() {
        HttpInboundConnectorService.HttpConnectorKey key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);

        httpInboundConnectorService.addListener(key, body -> invoked.set(true));
        httpInboundConnectorService.removeListener(key);
        httpInboundConnectorService.invoke(key, Map.of());

        assertFalse(invoked.get());
    }

    @Test
    void testInvokeUnknownKey() {
        HttpInboundConnectorService.HttpConnectorKey key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        // Should not throw exception
        httpInboundConnectorService.invoke(key, Map.of());
    }
}
