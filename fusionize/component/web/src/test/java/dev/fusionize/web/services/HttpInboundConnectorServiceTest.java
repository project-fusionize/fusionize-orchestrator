package dev.fusionize.web.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpInboundConnectorServiceTest {

    private HttpInboundConnectorService service;

    @BeforeEach
    void setUp() {
        service = new HttpInboundConnectorService();
    }

    @Test
    void addAndInvokeListener() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);
        Map<String, Object> payload = Map.of("data", "test");

        service.addListener(key, body -> {
            invoked.set(true);
            assertEquals("test", body.get("data"));
        });

        service.invoke(key, payload);
        assertTrue(invoked.get());
    }

    @Test
    void removeListener() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        AtomicBoolean invoked = new AtomicBoolean(false);

        service.addListener(key, body -> invoked.set(true));
        service.removeListener(key);
        service.invoke(key, Map.of());

        assertFalse(invoked.get());
    }

    @Test
    void invokeUnknownKey_doesNotThrow() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        assertDoesNotThrow(() -> service.invoke(key, Map.of()));
    }

    @Test
    void hasListener_returnsTrueWhenRegistered() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        service.addListener(key, body -> {});

        assertTrue(service.hasListener(key));
    }

    @Test
    void hasListener_returnsFalseWhenNotRegistered() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        assertFalse(service.hasListener(key));
    }

    @Test
    void hasListener_returnsFalseAfterRemoval() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        service.addListener(key, body -> {});
        service.removeListener(key);

        assertFalse(service.hasListener(key));
    }

    @Test
    void replaceListener_overwritesPrevious() {
        var key = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        AtomicReference<String> which = new AtomicReference<>("");

        service.addListener(key, body -> which.set("first"));
        service.addListener(key, body -> which.set("second"));
        service.invoke(key, Map.of());

        assertEquals("second", which.get());
    }

    @Test
    void differentKeys_areIndependent() {
        var key1 = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        var key2 = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node2");
        AtomicBoolean invoked1 = new AtomicBoolean(false);
        AtomicBoolean invoked2 = new AtomicBoolean(false);

        service.addListener(key1, body -> invoked1.set(true));
        service.addListener(key2, body -> invoked2.set(true));
        service.invoke(key1, Map.of());

        assertTrue(invoked1.get());
        assertFalse(invoked2.get());
    }

    @Test
    void keyEquality_worksCorrectly() {
        var key1 = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");
        var key2 = new HttpInboundConnectorService.HttpConnectorKey("wf1", "node1");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }
}
