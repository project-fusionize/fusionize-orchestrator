package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.context.ContextFactory;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    private final OrchestratorComponentDispatcher componentDispatcher;
    private final OrchestratorWorkflowNavigator workflowNavigator;

    public Orchestrator(WorkflowRegistry workflowRegistry,
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
        orchestrate(workflow);
    }

    public void orchestrate(Workflow workflow) {
        WorkflowExecution we = WorkflowExecution.of(workflow);
        workflowExecutionRegistry.deleteIdlesFor(workflow.getWorkflowId());
        List<WorkflowNodeExecution> nodes = workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n, ContextFactory.empty()))
                .peek(ne -> we.getNodes().add(ne)).toList();
        workflowExecutionRegistry.register(we);
        nodes.forEach(ne -> componentDispatcher.dispatchActivation(we, ne));
    }

    private void proceedExecution(WorkflowExecution we, WorkflowNodeExecution ne) {
        workflowNavigator.navigate(we, ne, (
                WorkflowExecution nextWe, WorkflowNodeExecution nextNe) -> {
            workflowExecutionRegistry.register(nextWe);
            nextNe.getChildren().forEach(cne -> componentDispatcher.dispatchActivation(nextWe, cne));
        });

    }

    public void onActivated(ActivationResponseEvent activationResponseEvent) {
        OrchestrationEvent.EventContext oc = activationResponseEvent.getOrchestrationEventContext();
        if (activationResponseEvent.getException() != null) {
            log.error("Error onActivated -> {}", activationResponseEvent.getException().getMessage(),
                    activationResponseEvent.getException());
            // todo handle escalation or compensation
            oc.nodeExecution().setState(WorkflowNodeExecutionState.FAILED);
            if(oc.nodeExecution().getWorkflowNode().getType().equals(WorkflowNodeType.START)){
                oc.workflowExecution().setStatus(WorkflowExecutionStatus.ERROR);
            }
        } else {
            componentDispatcher.dispatchInvocation(oc.workflowExecution(), oc.nodeExecution());
            if(oc.nodeExecution().getWorkflowNode().getType().equals(WorkflowNodeType.WAIT)){
                oc.nodeExecution().setState(WorkflowNodeExecutionState.WAITING);
            }else if(!oc.nodeExecution().getWorkflowNode().getType().equals(WorkflowNodeType.START)){
                oc.nodeExecution().setState(WorkflowNodeExecutionState.WORKING);
            }
            workflowExecutionRegistry.updateStatus(oc.workflowExecution().getWorkflowExecutionId(),
                    oc.workflowExecution().getStatus());
            workflowExecutionRegistry.updateNodeExecution(oc.workflowExecution().getWorkflowExecutionId(),
                    oc.nodeExecution());

        }
    }

    public void onInvoked(InvocationResponseEvent invocationResponseEvent) {
        OrchestrationEvent.EventContext oc = invocationResponseEvent.getOrchestrationEventContext();

        if (invocationResponseEvent.getException() != null) {
            log.error("Error onInvoked -> {}", invocationResponseEvent.getException().getMessage(),
                    invocationResponseEvent.getException());
            // todo handle escalation or compensation
            oc.nodeExecution().setState(WorkflowNodeExecutionState.FAILED);
            workflowExecutionRegistry.updateNodeExecution(oc.workflowExecution().getWorkflowExecutionId(),
                    oc.nodeExecution());
            if(oc.nodeExecution().getWorkflowNode().getType().equals(WorkflowNodeType.START)){
                oc.workflowExecution().setStatus(WorkflowExecutionStatus.ERROR);
                workflowExecutionRegistry.updateStatus(oc.workflowExecution().getWorkflowExecutionId(),
                        oc.workflowExecution().getStatus());
            }
            workflowExecutionRegistry.register(oc.workflowExecution());
            return;
        }
        log.info(invocationResponseEvent.getContext().toString());
        oc.nodeExecution().setStageContext(invocationResponseEvent.getContext());
        proceedExecution(oc.workflowExecution(), oc.nodeExecution());
    }


}
