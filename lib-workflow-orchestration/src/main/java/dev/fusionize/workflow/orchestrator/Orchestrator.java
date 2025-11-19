package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.context.WorkflowContextFactory;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private final EventPublisher<Event> eventPublisher;
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    private final OrchestratorDecisionEngine decisionEngine;
    private final OrchestratorComponentDispatcher componentDispatcher;
    private final OrchestratorWorkflowNavigator workflowNavigator;

    public Orchestrator(EventPublisher<Event> publisher,
                        WorkflowRepoRegistry workflowRegistry,
                        WorkflowExecutionRepoRegistry workflowExecutionRegistry,
                        OrchestratorDecisionEngine decisionEngine,
                        OrchestratorComponentDispatcher componentDispatcher,
                        OrchestratorWorkflowNavigator workflowNavigator) {
        this.eventPublisher = publisher;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
        this.decisionEngine = decisionEngine;
        this.componentDispatcher = componentDispatcher;
        this.workflowNavigator = workflowNavigator;
    }

    public void orchestrate(String workflowId) {
        Workflow workflow = workflowRegistry.getWorkflow(workflowId);
        WorkflowExecution we = WorkflowExecution.of(workflow);
        //todo check and re-use idle execution
        List<WorkflowNodeExecution> nodes = workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.empty()))
                .peek(ne -> we.getNodes().add(ne)).toList();
        workflowExecutionRegistry.register(we);
        nodes.forEach(ne -> requestActivation(we, ne));

    }

    private void proceed(WorkflowExecution we, WorkflowNodeExecution ne) {
        List<WorkflowNodeExecution> nodeExecutions = workflowNavigator.navigate(we, ne);

        workflowExecutionRegistry.register(we);
        WorkflowExecution finalWorkflowExecution = we;
        nodeExecutions.forEach(cne -> requestActivation(finalWorkflowExecution, cne));
    }

    private void requestActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
        componentDispatcher.dispatchActivation(we, ne,
                (workflowExecution, nodeExecution) -> componentDispatcher.dispatchInvocation(workflowExecution, nodeExecution,
                        this::handleInvocationSuccess, this::handleFailure),
                this::handleFailure);
    }

    public void onActivated(ActivationResponseEvent activationResponseEvent) {
        if (activationResponseEvent.getException() != null) {
            //todo handle exception
            log.error("Error -> {}", activationResponseEvent.getException().getMessage(), activationResponseEvent.getException());

        } else {
            OrchestrationEvent.EventContext oc = activationResponseEvent.getOrchestrationEventContext();
            componentDispatcher.dispatchInvocation(oc.workflowExecution(), oc.nodeExecution(),
                    this::handleInvocationSuccess, this::handleFailure);
        }
    }

    public void onInvoked(InvocationResponseEvent invocationResponseEvent) {
        if (invocationResponseEvent.getException() != null) {
            //todo handle exception
            log.error(invocationResponseEvent.getException().getMessage(), invocationResponseEvent.getException());
            return;
        }
        log.info(invocationResponseEvent.getContext().toString());
        OrchestrationEvent.EventContext oc = invocationResponseEvent.getOrchestrationEventContext();
        oc.nodeExecution().setStageContext(invocationResponseEvent.getContext());
        proceed(oc.workflowExecution(), oc.nodeExecution());
    }

    private void handleInvocationSuccess(WorkflowExecution we, WorkflowNodeExecution ne) {
        proceed(we, ne);
    }

    private void handleFailure(Exception ex, WorkflowNodeExecution ne) {
        log.error("Error executing node {}: {}", ne.getWorkflowNodeExecutionId(), ex.getMessage(), ex);
    }
}
