package dev.fusionize.web;

import dev.fusionize.web.services.HttpInboundConnectorService;
import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HttpInboundConnectorTest {

    private HttpInboundConnector myConnector;
    private HttpInboundConnectorService httpInboundConnectorService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        httpInboundConnectorService = mock(HttpInboundConnectorService.class);
        myConnector = new HttpInboundConnector(httpInboundConnectorService);
        context = new Context();
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowId("wf1");
        runtimeData.setWorkflowNodeKey("node1");
        context.setRuntimeData(runtimeData);
        emitter = new TestEmitter();
    }

    @Test
    void testRun_RegistersListenerAndHandlesCallback() {
        // Run
        myConnector.run(context, emitter);

        // Verify listener registered
        ArgumentCaptor<HttpInboundConnectorService.HttpConnectorKey> keyCaptor = ArgumentCaptor.forClass(HttpInboundConnectorService.HttpConnectorKey.class);
        ArgumentCaptor<Consumer<Map<String, Object>>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(httpInboundConnectorService).addListener(keyCaptor.capture(), listenerCaptor.capture());

        HttpInboundConnectorService.HttpConnectorKey key = keyCaptor.getValue();
        assertEquals("wf1", key.workflowKey());
        assertEquals("node1", key.workflowNodeKey());

        // Simulate callback
        Map<String, Object> payload = Map.of("data", "value");
        listenerCaptor.getValue().accept(payload);

        // Verify context updated and success emitted
        assertEquals("value", context.getData().get("data"));
        assertTrue(emitter.successCalled);

        // Verify cleanup
//        verify(httpInboundConnectorService).removeListener(eq(key));
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        boolean failureCalled = false;

        @Override
        public void success(Context updatedContext) {
            successCalled = true;
        }

        @Override
        public void failure(Exception ex) {
            failureCalled = true;
            ex.printStackTrace();
        }

        @Override
        public ComponentUpdateEmitter.Logger logger() {
            return (message, level, throwable) -> {
                System.out.println("[" + level + "] " + message);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            };
        }

        @Override
        public InteractionLogger interactionLogger() {
            return (Object content,
                    String actor,
                    WorkflowInteraction.InteractionType type,
                    WorkflowInteraction.Visibility visibility) ->  System.out.println("[" + actor + "] " + content);

        }
    }
}
