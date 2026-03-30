package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationActivateResponseEventHandlerTest {

    @Mock
    private Orchestrator orchestrator;

    @Mock
    private WorkflowRepoRegistry workflowRegistry;

    @Mock
    private WorkflowExecutionRepoRegistry workflowExecutionRegistry;

    @InjectMocks
    private OrchestrationActivateResponseEventHandler handler;

    @Test
    void shouldHandle_whenAllConditionsMet() {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .build());

        // expectation
        boolean result = handler.shouldHandle(event);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotHandle_whenEventIsNull() {
        // setup
        // no setup needed

        // expectation
        boolean result = handler.shouldHandle(null);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenProcessedDateIsSet() {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .processedAt(new Date())
                .build());

        // expectation
        boolean result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenEventClassDoesNotMatch() {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .build());
        doReturn("some.other.EventClass").when(event).getEventClass();

        // expectation
        boolean result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHandle_whenOriginIsNotRuntimeEngine() {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .build());

        // expectation
        boolean result = handler.shouldHandle(event);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldCallOrchestratorOnActivated() throws Exception {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .build());
        doNothing().when(event).ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);

        // expectation
        handler.handle(event);

        // validation
        verify(event).ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        verify(orchestrator).onActivated(event);
    }

    @Test
    void shouldReturnNull_fromHandle() throws Exception {
        // setup
        var event = spy(ActivationResponseEvent.builder(this)
                .origin(OrchestrationEvent.Origin.RUNTIME_ENGINE)
                .build());
        doNothing().when(event).ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);

        // expectation
        Event result = handler.handle(event);

        // validation
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnCorrectEventType() {
        // setup
        // no setup needed

        // expectation
        Class<ActivationResponseEvent> eventType = handler.getEventType();

        // validation
        assertThat(eventType).isEqualTo(ActivationResponseEvent.class);
    }
}
