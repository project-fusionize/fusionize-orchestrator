package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.component.ComponentConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

class ComponentRuntimeEngineTest {

    @Mock
    private ComponentRuntimeRegistry registry;
    @Mock
    private EventPublisher<Event> eventPublisher;
    @Mock
    private WorkflowLogger workflowLogger;
    @Mock
    private ExecutorService executor;
    @Mock
    private ComponentRuntime componentRuntime;

    private ComponentRuntimeEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new ComponentRuntimeEngine(registry, eventPublisher, workflowLogger, executor);

        // Mock executor to run immediately
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    void activateComponent_WhenComponentNotFound_ShouldReturnExceptionEvent() {
        ActivationRequestEvent request = createActivationRequest();
        when(registry.get(anyString(), any())).thenReturn(Optional.empty());

        ActivationResponseEvent response = engine.activateComponent(request);

        assertNotNull(response);
        assertNotNull(response.getException());
        assertTrue(response.getException().getMessage().contains(ComponentRuntimeEngine.ERR_CODE_COMP_NOT_FOUND));
    }

    @Test
    void activateComponent_WhenComponentFound_ShouldExecuteAndPublishSuccess() {
        ActivationRequestEvent request = createActivationRequest();
        when(registry.get(anyString(), any())).thenReturn(Optional.of(componentRuntime));

        // Capture the emitter passed to canActivate
        doAnswer(invocation -> {
            ComponentUpdateEmitter emitter = invocation.getArgument(1);
            emitter.success(new Context());
            return null;
        }).when(componentRuntime).canActivate(any(Context.class), any(ComponentUpdateEmitter.class));

        engine.activateComponent(request);

        // Verify executor ran
        verify(executor).execute(any(Runnable.class)); // CompletableFuture.runAsync uses execute

        // Verify event published
        ArgumentCaptor<ActivationResponseEvent> captor = ArgumentCaptor.forClass(ActivationResponseEvent.class);
        verify(eventPublisher).publish(captor.capture());

        ActivationResponseEvent publishedEvent = captor.getValue();
        assertNotNull(publishedEvent.getContext());
        assertNull(publishedEvent.getException());
    }

    @Test
    void invokeComponent_WhenComponentNotFound_ShouldReturnExceptionEvent() {
        InvocationRequestEvent request = createInvocationRequest();
        when(registry.get(anyString(), any())).thenReturn(Optional.empty());

        InvocationResponseEvent response = engine.invokeComponent(request);

        assertNotNull(response);
        assertNotNull(response.getException());
        assertTrue(response.getException().getMessage().contains(ComponentRuntimeEngine.ERR_CODE_COMP_NOT_FOUND));
    }

    @Test
    void invokeComponent_WhenComponentFound_ShouldExecuteAndPublishSuccess() {
        InvocationRequestEvent request = createInvocationRequest();
        when(registry.get(anyString(), any())).thenReturn(Optional.of(componentRuntime));

        doAnswer(invocation -> {
            ComponentUpdateEmitter emitter = invocation.getArgument(1);
            emitter.success(new Context());
            return null;
        }).when(componentRuntime).run(any(Context.class), any(ComponentUpdateEmitter.class));

        engine.invokeComponent(request);

        verify(executor).execute(any(Runnable.class));

        ArgumentCaptor<InvocationResponseEvent> captor = ArgumentCaptor.forClass(InvocationResponseEvent.class);
        verify(eventPublisher).publish(captor.capture());

        InvocationResponseEvent publishedEvent = captor.getValue();
        assertNotNull(publishedEvent.getContext());
        assertNull(publishedEvent.getException());
    }

    private ActivationRequestEvent createActivationRequest() {
        return (ActivationRequestEvent) createOrchestrationEvent(new ActivationRequestEvent());
    }

    private InvocationRequestEvent createInvocationRequest() {
        return (InvocationRequestEvent) createOrchestrationEvent(new InvocationRequestEvent());
    }

    private OrchestrationEvent createOrchestrationEvent(OrchestrationEvent event) {
        event.setComponent("test-component");
        event.setContext(new Context());

        WorkflowNode node = new WorkflowNode();
        node.setComponentConfig(new ComponentConfig());
        node.setComponent("test-component");

        WorkflowNodeExecution nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNode(node);

        WorkflowExecution workflowExecution = new WorkflowExecution();

        OrchestrationEvent.EventContext context = new OrchestrationEvent.EventContext(
                workflowExecution, nodeExecution);

        event.setOrchestrationEventContext(context);
        return event;
    }
}
