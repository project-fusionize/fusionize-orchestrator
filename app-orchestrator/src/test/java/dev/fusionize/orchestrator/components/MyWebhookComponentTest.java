package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.WebhookService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MyWebhookComponentTest {

    private MyWebhookComponent myWebhookComponent;
    private WebhookService webhookService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        webhookService = mock(WebhookService.class);
        myWebhookComponent = new MyWebhookComponent(webhookService);
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
        myWebhookComponent.run(context, emitter);

        // Verify listener registered
        ArgumentCaptor<WebhookService.WebhookKey> keyCaptor = ArgumentCaptor.forClass(WebhookService.WebhookKey.class);
        ArgumentCaptor<Consumer<Map<String, Object>>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(webhookService).addListener(keyCaptor.capture(), listenerCaptor.capture());

        WebhookService.WebhookKey key = keyCaptor.getValue();
        assertEquals("wf1", key.workflowKey());
        assertEquals("node1", key.workflowNodeKey());

        // Simulate callback
        Map<String, Object> payload = Map.of("data", "value");
        listenerCaptor.getValue().accept(payload);

        // Verify context updated and success emitted
        assertEquals("value", context.getData().get("data"));
        assertTrue(emitter.successCalled);

        // Verify cleanup
        verify(webhookService).removeListener(eq(key));
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
    }
}
