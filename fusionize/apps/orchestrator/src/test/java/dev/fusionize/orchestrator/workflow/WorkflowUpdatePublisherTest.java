package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import dev.fusionize.common.utility.Debouncer;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowUpdatePublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WorkflowRegistry workflowRegistry;

    @Mock
    private WorkflowExecutionRegistry workflowExecutionRegistry;

    @Mock
    private EventListener<Event> eventListener;

    @Mock
    private Debouncer<String> debouncer;

    private WorkflowUpdatePublisher workflowUpdatePublisher;

    @BeforeEach
    void setUp() {
        workflowUpdatePublisher = new WorkflowUpdatePublisher(
                messagingTemplate,
                workflowRegistry,
                workflowExecutionRegistry,
                eventListener,
                Runnable::run
        );
        workflowUpdatePublisher.debouncer = debouncer;
    }

    @Test
    void onEvent_shouldDebounceUpdate_whenOrchestrationEvent() {
        // Arrange
        String executionId = "exec-123";
        OrchestrationEvent event = mock(OrchestrationEvent.class);
        when(event.getWorkflowExecutionId()).thenReturn(executionId);

        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowExecutionId(executionId);
        execution.setWorkflowId("123");
        when(workflowExecutionRegistry.getWorkflowExecution(executionId)).thenReturn(execution);

        // Act
        workflowUpdatePublisher.onEvent(event);

        // Assert
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(debouncer).debounce(eq(executionId), runnableCaptor.capture());

        // Execute the captured runnable to trigger the logic inside
        runnableCaptor.getValue().run();

        verify(messagingTemplate).convertAndSend(eq("/topic/1.0.workflow-executions.123"), eq(execution));
    }

    @Test
    void onEvent_shouldNotSendUpdate_whenNotOrchestrationEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        workflowUpdatePublisher.onEvent(event);

        // Assert
        verifyNoInteractions(debouncer);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void onEvent_shouldNotSendUpdate_whenExecutionNotFound() {
        // Arrange
        String executionId = "exec-123";
        OrchestrationEvent event = mock(OrchestrationEvent.class);
        when(event.getWorkflowExecutionId()).thenReturn(executionId);

        when(workflowExecutionRegistry.getWorkflowExecution(executionId)).thenReturn(null);

        // Act
        workflowUpdatePublisher.onEvent(event);

        // Assert
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(debouncer).debounce(eq(executionId), runnableCaptor.capture());

        // Execute the captured runnable
        runnableCaptor.getValue().run();

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
