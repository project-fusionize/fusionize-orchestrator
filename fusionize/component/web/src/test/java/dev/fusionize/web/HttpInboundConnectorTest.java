package dev.fusionize.web;

import dev.fusionize.web.services.HttpInboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpInboundConnectorTest {

    private HttpInboundConnector connector;
    private HttpInboundConnectorService httpService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpInboundConnectorService.class);
        connector = new HttpInboundConnector(httpService);
        context = new Context();
        emitter = new TestEmitter();
    }

    private void setRuntimeData(String workflowId, String workflowDomain, String nodeKey) {
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowId(workflowId);
        runtimeData.setWorkflowDomain(workflowDomain);
        runtimeData.setWorkflowNodeKey(nodeKey);
        context.setRuntimeData(runtimeData);
    }

    // --- configure ---

    @Test
    void configure_isNoOp() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        assertDoesNotThrow(() -> connector.configure(config));
    }

    // --- canActivate ---

    @Test
    void canActivate_alwaysSucceeds() {
        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
        assertFalse(emitter.failureCalled);
    }

    // --- run ---

    @Test
    void run_registersListenerAndHandlesCallback() {
        setRuntimeData("wf1", null, "node1");

        connector.run(context, emitter);

        ArgumentCaptor<HttpInboundConnectorService.HttpConnectorKey> keyCaptor =
                ArgumentCaptor.forClass(HttpInboundConnectorService.HttpConnectorKey.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Map<String, Object>>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(httpService).addListener(keyCaptor.capture(), listenerCaptor.capture());

        HttpInboundConnectorService.HttpConnectorKey key = keyCaptor.getValue();
        assertEquals("wf1", key.workflowKey());
        assertEquals("node1", key.workflowNodeKey());

        // Simulate callback
        Map<String, Object> payload = Map.of("data", "value", "count", 42);
        listenerCaptor.getValue().accept(payload);

        assertEquals("value", context.getData().get("data"));
        assertEquals(42, context.getData().get("count"));
        assertTrue(emitter.successCalled);
    }

    @Test
    void run_usesWorkflowDomainOverId() {
        setRuntimeData("wf-id", "wf-domain", "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpInboundConnectorService.HttpConnectorKey> keyCaptor =
                ArgumentCaptor.forClass(HttpInboundConnectorService.HttpConnectorKey.class);
        verify(httpService).addListener(keyCaptor.capture(), any());

        assertEquals("wf-domain", keyCaptor.getValue().workflowKey());
    }

    @Test
    void run_fallsBackToWorkflowId_whenDomainNull() {
        setRuntimeData("wf-id", null, "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpInboundConnectorService.HttpConnectorKey> keyCaptor =
                ArgumentCaptor.forClass(HttpInboundConnectorService.HttpConnectorKey.class);
        verify(httpService).addListener(keyCaptor.capture(), any());

        assertEquals("wf-id", keyCaptor.getValue().workflowKey());
    }

    @Test
    void run_failsWhenWorkflowKeyNull() {
        setRuntimeData(null, null, "node1");

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
        verify(httpService, never()).addListener(any(), any());
    }

    @Test
    void run_failsWhenNodeKeyNull() {
        setRuntimeData("wf1", null, null);

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
        verify(httpService, never()).addListener(any(), any());
    }

    @Test
    void run_failsWhenBothKeysNull() {
        setRuntimeData(null, null, null);

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        verify(httpService, never()).addListener(any(), any());
    }

    @Test
    void run_callbackMergesIntoExistingContextData() {
        setRuntimeData("wf1", null, "node1");
        context.set("existing", "keep-me");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Map<String, Object>>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(httpService).addListener(any(), listenerCaptor.capture());

        listenerCaptor.getValue().accept(Map.of("new", "data"));

        assertEquals("keep-me", context.getData().get("existing"));
        assertEquals("data", context.getData().get("new"));
    }
}
