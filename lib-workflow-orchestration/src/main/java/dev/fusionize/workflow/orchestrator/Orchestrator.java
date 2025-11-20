package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.context.WorkflowContextFactory;
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
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    private final OrchestratorComponentDispatcher componentDispatcher;
    private final OrchestratorWorkflowNavigator workflowNavigator;

    public Orchestrator(WorkflowRepoRegistry workflowRegistry,
            WorkflowExecutionRepoRegistry workflowExecutionRegistry,
            OrchestratorComponentDispatcher componentDispatcher,
            OrchestratorWorkflowNavigator workflowNavigator) {
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
        this.componentDispatcher = componentDispatcher;
        this.workflowNavigator = workflowNavigator;
    }

    public void orchestrate(String workflowId) {
        Workflow workflow = workflowRegistry.getWorkflow(workflowId);
        WorkflowExecution we = WorkflowExecution.of(workflow);
        // todo check and re-use idle execution
        List<WorkflowNodeExecution> nodes = workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.empty()))
                .peek(ne -> we.getNodes().add(ne)).toList();
        workflowExecutionRegistry.register(we);
        nodes.forEach(ne -> requestActivation(we, ne));

    }

    private void proceedExecution(WorkflowExecution we, WorkflowNodeExecution ne) {
        workflowNavigator.navigate(we, ne, (
                WorkflowExecution nextWe, WorkflowNodeExecution nextNe
        )->{
            workflowExecutionRegistry.register(nextWe);
            nextNe.getChildren().forEach(cne -> requestActivation(nextWe, cne));
        });

    }

    private void requestActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
        componentDispatcher.dispatchActivation(we, ne, this::handleActivationSuccess, this::handleFailure);
    }

    public void onActivated(ActivationResponseEvent activationResponseEvent) {
        if (activationResponseEvent.getException() != null) {
            // todo handle exception
            log.error("Error onActivated -> {}", activationResponseEvent.getException().getMessage(),
                    activationResponseEvent.getException());

        } else {
            OrchestrationEvent.EventContext oc = activationResponseEvent.getOrchestrationEventContext();
            requestInvocation(oc.workflowExecution(), oc.nodeExecution());
        }
    }

    private void requestInvocation(WorkflowExecution we, WorkflowNodeExecution ne) {
        componentDispatcher.dispatchInvocation( we, ne, this::handleInvocationSuccess, this::handleFailure);
    }

    public void onInvoked(InvocationResponseEvent invocationResponseEvent) {
        if (invocationResponseEvent.getException() != null) {
            // todo handle exception
            log.error("Error onInvoked -> {}", invocationResponseEvent.getException().getMessage(),
                    invocationResponseEvent.getException());
            return;
        }
        log.info(invocationResponseEvent.getContext().toString());
        OrchestrationEvent.EventContext oc = invocationResponseEvent.getOrchestrationEventContext();
        oc.nodeExecution().setStageContext(invocationResponseEvent.getContext());
        handleInvocationSuccess(oc.workflowExecution(), oc.nodeExecution());
    }

    private void handleActivationSuccess(WorkflowExecution we, WorkflowNodeExecution ne) {
        requestInvocation( we, ne);
    }

    private void handleInvocationSuccess(WorkflowExecution we, WorkflowNodeExecution ne) {
        proceedExecution(we, ne);
    }

    private void handleFailure(Exception ex, WorkflowNodeExecution ne) {
        log.error("Error executing node {}: {}", ne.getWorkflowNodeExecutionId(), ex.getMessage(), ex);
    }
}
