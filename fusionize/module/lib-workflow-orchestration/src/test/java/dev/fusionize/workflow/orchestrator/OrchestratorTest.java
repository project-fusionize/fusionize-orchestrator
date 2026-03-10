package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextFactory;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrchestratorTest {

    private WorkflowRegistry workflowRegistry;
    private WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    private OrchestratorComponentDispatcher componentDispatcher;
    private OrchestratorWorkflowNavigator workflowNavigator;
    private Orchestrator orchestrator;

    @BeforeEach
    void setUp() {
        workflowRegistry = mock(WorkflowRegistry.class);
        workflowExecutionRegistry = mock(WorkflowExecutionRepoRegistry.class);
        componentDispatcher = mock(OrchestratorComponentDispatcher.class);
        workflowNavigator = mock(OrchestratorWorkflowNavigator.class);
        orchestrator = new Orchestrator(workflowRegistry, workflowExecutionRegistry,
                componentDispatcher, workflowNavigator);
    }

    @Test
    void orchestrateByIdLooksUpWorkflow() {
        Workflow workflow = buildSimpleWorkflow();
        when(workflowRegistry.getWorkflow("wf-1")).thenReturn(workflow);

        orchestrator.orchestrate("wf-1");

        verify(workflowRegistry).getWorkflow("wf-1");
        verify(workflowExecutionRegistry).deleteIdlesFor(workflow.getWorkflowId());
        verify(workflowExecutionRegistry).register(any(WorkflowExecution.class));
        verify(componentDispatcher, atLeastOnce()).dispatchActivation(any(), any());
    }

    @Test
    void orchestrateCreatesExecutionAndDispatchesActivations() {
        Workflow workflow = buildSimpleWorkflow();

        orchestrator.orchestrate(workflow);

        verify(workflowExecutionRegistry).deleteIdlesFor(workflow.getWorkflowId());

        ArgumentCaptor<WorkflowExecution> execCaptor = ArgumentCaptor.forClass(WorkflowExecution.class);
        verify(workflowExecutionRegistry).register(execCaptor.capture());

        WorkflowExecution captured = execCaptor.getValue();
        assertEquals(workflow.getWorkflowId(), captured.getWorkflowId());
        assertFalse(captured.getNodes().isEmpty());

        // One activation per root node
        verify(componentDispatcher, times(workflow.getNodes().size()))
                .dispatchActivation(any(WorkflowExecution.class), any(WorkflowNodeExecution.class));
    }

    @Test
    void onActivatedWithExceptionSetsFailedState() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());

        ActivationResponseEvent event = ActivationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .exception(new RuntimeException("activation failed"))
                .build();

        orchestrator.onActivated(event);

        assertEquals(WorkflowNodeExecutionState.FAILED, ne.getState());
    }

    @Test
    void onActivatedWithExceptionOnStartNodeSetsErrorStatus() {
        WorkflowNode startNode = WorkflowNode.builder()
                .workflowNodeId("start-1")
                .type(WorkflowNodeType.START)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(startNode, Context.builder().build());

        ActivationResponseEvent event = ActivationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .exception(new RuntimeException("start activation failed"))
                .build();

        orchestrator.onActivated(event);

        assertEquals(WorkflowNodeExecutionState.FAILED, ne.getState());
        assertEquals(WorkflowExecutionStatus.ERROR, we.getStatus());
    }

    @Test
    void onActivatedSuccessDispatchesInvocationAndSetsWorkingState() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());

        ActivationResponseEvent event = ActivationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .build();

        orchestrator.onActivated(event);

        verify(componentDispatcher).dispatchInvocation(we, ne);
        assertEquals(WorkflowNodeExecutionState.WORKING, ne.getState());
        verify(workflowExecutionRegistry).updateStatus(eq("exec-1"), any());
        verify(workflowExecutionRegistry).updateNodeExecution(eq("exec-1"), eq(ne));
    }

    @Test
    void onActivatedSuccessOnWaitNodeSetsWaitingState() {
        WorkflowNode waitNode = WorkflowNode.builder()
                .workflowNodeId("wait-1")
                .type(WorkflowNodeType.WAIT)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(waitNode, Context.builder().build());

        ActivationResponseEvent event = ActivationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .build();

        orchestrator.onActivated(event);

        assertEquals(WorkflowNodeExecutionState.WAITING, ne.getState());
    }

    @Test
    void onInvokedWithExceptionSetsFailedState() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());

        InvocationResponseEvent event = InvocationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .exception(new RuntimeException("invocation failed"))
                .build();

        orchestrator.onInvoked(event);

        assertEquals(WorkflowNodeExecutionState.FAILED, ne.getState());
        verify(workflowExecutionRegistry).updateNodeExecution(eq("exec-1"), eq(ne));
        verify(workflowExecutionRegistry).register(we);
    }

    @Test
    void onInvokedWithExceptionOnStartNodeSetsErrorStatus() {
        WorkflowNode startNode = WorkflowNode.builder()
                .workflowNodeId("start-1")
                .type(WorkflowNodeType.START)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(startNode, Context.builder().build());

        InvocationResponseEvent event = InvocationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .exception(new RuntimeException("start invocation failed"))
                .build();

        orchestrator.onInvoked(event);

        assertEquals(WorkflowNodeExecutionState.FAILED, ne.getState());
        assertEquals(WorkflowExecutionStatus.ERROR, we.getStatus());
        verify(workflowExecutionRegistry).updateStatus(eq("exec-1"), eq(WorkflowExecutionStatus.ERROR));
    }

    @Test
    void onInvokedSuccessProceedsExecution() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflowId("wf-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());

        Context resultContext = Context.builder().build();
        InvocationResponseEvent event = InvocationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .context(resultContext)
                .build();

        orchestrator.onInvoked(event);

        assertEquals(resultContext, ne.getStageContext());
        verify(workflowNavigator).navigate(eq(we), eq(ne), any());
    }

    @Test
    void onInvokedSkipsWhenNodeAlreadyDone() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());
        ne.setState(WorkflowNodeExecutionState.DONE);

        InvocationResponseEvent event = InvocationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .context(Context.builder().build())
                .build();

        orchestrator.onInvoked(event);

        verify(workflowNavigator, never()).navigate(any(), any(), any());
    }

    @Test
    void onInvokedSkipsWhenNodeAlreadyFailed() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("task-1")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        WorkflowExecution we = new WorkflowExecution();
        we.setWorkflowExecutionId("exec-1");
        we.setWorkflow(new Workflow());
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());
        ne.setState(WorkflowNodeExecutionState.FAILED);

        InvocationResponseEvent event = InvocationResponseEvent.builder(this)
                .orchestrationEventContext(we, ne)
                .context(Context.builder().build())
                .build();

        orchestrator.onInvoked(event);

        verify(workflowNavigator, never()).navigate(any(), any(), any());
    }

    @Test
    void replayExecutionDispatchesActivation() {
        WorkflowNode node = WorkflowNode.builder()
                .workflowNodeId("node-1")
                .workflowNodeKey("taskNode")
                .type(WorkflowNodeType.TASK)
                .component("comp")
                .build();
        Workflow workflow = Workflow.builder("test")
                .withWorkflowId("wf-1")
                .addNode(node)
                .build();

        WorkflowExecution we = WorkflowExecution.of(workflow);
        WorkflowNodeExecution ne = WorkflowNodeExecution.of(node, Context.builder().build());
        we.getNodes().add(ne);

        when(workflowRegistry.getWorkflow("wf-1")).thenReturn(workflow);
        when(workflowExecutionRegistry.getWorkflowExecution(we.getWorkflowExecutionId())).thenReturn(we);

        orchestrator.replayExecution("wf-1", we.getWorkflowExecutionId(), ne.getWorkflowNodeExecutionId());

        verify(workflowExecutionRegistry).register(we);
        verify(componentDispatcher).dispatchActivation(eq(we), eq(ne));
        assertTrue(ne.getChildren().isEmpty());
    }

    private Workflow buildSimpleWorkflow() {
        WorkflowNode start = WorkflowNode.builder()
                .workflowNodeId("start")
                .type(WorkflowNodeType.START)
                .component("test.start")
                .build();
        return Workflow.builder("test")
                .withWorkflowId("wf-1")
                .addNode(start)
                .build();
    }
}
