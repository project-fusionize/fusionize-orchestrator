package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.component.local.LocalComponentBundle;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.logging.WorkflowLogRepoLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrchestratorComponentDispatcherTest {

    private EventPublisher<Event> eventPublisher;
    private List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles;
    private OrchestratorComponentDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        eventPublisher = Mockito.mock(EventPublisher.class);
        localComponentBundles = new ArrayList<>();
        WorkflowLogRepoLogger workflowLogger = Mockito.mock(WorkflowLogRepoLogger.class);
        dispatcher = new OrchestratorComponentDispatcher(eventPublisher, localComponentBundles, workflowLogger);
    }

    @Test
    public void testDispatchActivation_Remote() {
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");

        WorkflowNode node = WorkflowNode.builder().workflowNodeId("node-1").component("remote-component").build();
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, WorkflowContext.builder().build());

        dispatcher.dispatchActivation(we, ne, (w, n) -> {
        }, (e, n) -> {
        });

        ArgumentCaptor<ActivationRequestEvent> captor = ArgumentCaptor.forClass(ActivationRequestEvent.class);
        verify(eventPublisher).publish(captor.capture());

        ActivationRequestEvent event = captor.getValue();
        assertEquals("exec-1", event.getWorkflowExecutionId());
        assertEquals("node-1", event.getWorkflowNodeId());
    }

    @Test
    public void testDispatchInvocation_Remote() {
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");

        WorkflowNode node = WorkflowNode.builder().workflowNodeId("node-1").component("remote-component").build();
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, WorkflowContext.builder().build());

        dispatcher.dispatchInvocation(we, ne, (w, n) -> {
        }, (e, n) -> {
        });

        ArgumentCaptor<InvocationRequestEvent> captor = ArgumentCaptor.forClass(InvocationRequestEvent.class);
        verify(eventPublisher).publish(captor.capture());

        InvocationRequestEvent event = captor.getValue();
        assertEquals("exec-1", event.getWorkflowExecutionId());
        assertEquals("node-1", event.getWorkflowNodeId());
    }
}
