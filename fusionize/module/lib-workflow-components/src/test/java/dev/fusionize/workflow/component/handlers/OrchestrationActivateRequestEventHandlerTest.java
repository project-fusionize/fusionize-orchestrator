package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrchestrationActivateRequestEventHandlerTest {

    @Mock
    private ComponentRuntimeEngine componentRuntimeEngine;

    @Mock
    private WorkflowRegistry workflowRegistry;

    @Mock
    private WorkflowExecutionRegistry workflowExecutionRegistry;

    @InjectMocks
    private OrchestrationActivateRequestEventHandler handler;

    @Test
    void shouldHandle_whenAllConditionsMet() {
        // setup
        var event = ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build();

        // expectation
        var result = handler.shouldHandle(event);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotHandle_whenEventIsNull() {
        // setup
        // no event created

        // expectation
        var result = handler.shouldHandle(null);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenProcessedDateIsSet() {
        // setup
        var event = ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build();
        event.setProcessedDate(new Date());

        // expectation
        var result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenEventClassDoesNotMatch() {
        // setup
        var event = ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build();
        event.setEventClass("wrong");

        // expectation
        var result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenOriginIsNotOrchestrator() {
        // setup
        var event = ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .build();

        // expectation
        var result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldCallComponentRuntimeEngineActivate() throws Exception {
        // setup
        var event = spy(ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build());
        doNothing().when(event).ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);

        // expectation
        handler.handle(event);

        // validation
        verify(componentRuntimeEngine).activateComponent(event);
    }

    @Test
    void shouldReturnActivationResponseFromEngine() throws Exception {
        // setup
        var event = spy(ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build());
        doNothing().when(event).ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        ActivationResponseEvent mockResponse = mock(ActivationResponseEvent.class);
        when(componentRuntimeEngine.activateComponent(event)).thenReturn(mockResponse);

        // expectation
        var result = handler.handle(event);

        // validation
        assertThat(result).isSameAs(mockResponse);
    }

    @Test
    void shouldReturnCorrectEventType() {
        // setup
        // no setup needed

        // expectation
        var result = handler.getEventType();

        // validation
        assertThat(result).isEqualTo(ActivationRequestEvent.class);
    }
}
