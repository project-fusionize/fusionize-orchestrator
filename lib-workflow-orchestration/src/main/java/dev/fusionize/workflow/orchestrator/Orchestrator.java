package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.OrchestrationEventContext;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Orchestrator {
    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private final EventPublisher<Event> eventPublisher;
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;

    public Orchestrator(EventPublisher<Event> publisher,
                        WorkflowRepoRegistry workflowRegistry,
                        WorkflowExecutionRepoRegistry workflowExecutionRegistry) {
        this.eventPublisher = publisher;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
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
        WorkflowNodeExecution originalExecutionNode = ne;
        ne.setState(WorkflowNodeExecutionState.DONE);
        List<WorkflowNodeExecution> nodeExecutions = filterChildren(ne).stream()
                .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.from(originalExecutionNode, n)))
                .toList();

        if(ne.getWorkflowNode().getType().equals(WorkflowNodeType.START)){
            we = we.renew();
            we.setStatus(WorkflowExecutionStatus.IN_PROGRESS);

            ne = we.getNodes().stream().filter(n -> n.getWorkflowNodeId().equals(originalExecutionNode.getWorkflowNodeId()))
                    .findFirst().orElse(ne.renew());
            ne.setState(WorkflowNodeExecutionState.DONE);
        }

        if(ne.getWorkflowNode().getType().equals(WorkflowNodeType.END)){
            we.setStatus(WorkflowExecutionStatus.SUCCESS);
        }

        ne.getChildren().addAll(nodeExecutions);

        WorkflowNodeExecution finalNe = ne;
        if(we.getWorkflow().getNodes().stream().anyMatch(n-> n.getWorkflowNodeId().equals(finalNe.getWorkflowNodeId()))) {
            we.getNodes().removeIf(cne-> cne.getWorkflowNodeExecutionId().equals(finalNe.getWorkflowNodeExecutionId()));
            we.getNodes().add(ne);
        }

        workflowExecutionRegistry.register(we);
        WorkflowExecution finalWorkflowExecution = we;
        nodeExecutions.forEach(cne -> requestActivation(finalWorkflowExecution, cne));
    }

    private List<WorkflowNode> filterChildren(WorkflowNodeExecution ne) {
        List<WorkflowNode> allChildren = ne.getWorkflowNode().getChildren();
        if (ne.getStageContext().getDecisions().isEmpty()) {
            return allChildren;
        }
        if (!WorkflowNodeType.DECISION.equals(ne.getWorkflowNode().getType())) {
            return allChildren;
        }
        WorkflowDecision lastDecision = ne.getStageContext().getDecisions().getLast();
        if (lastDecision.getDecisionNode() == null
                || ne.getWorkflowNode().getWorkflowNodeKey() == null) {
            return new ArrayList<>();
        }
        if (!lastDecision.getDecisionNode().equals(ne.getWorkflowNode().getWorkflowNodeKey())) {
            return new ArrayList<>();
        }
        return allChildren.stream().filter(n -> n.getWorkflowNodeKey() != null)
                .filter(n -> lastDecision.getOptionNodes().get(n.getWorkflowNodeKey())).toList();
    }

    private void requestActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
        ActivationRequestEvent activationRequestEvent =
                ActivationRequestEvent.builder(this)
                        .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .orchestrationEventContext(we, ne)
                        .component(ne.getWorkflowNode().getComponent())
                        .context(ne.getStageContext()).build();
        eventPublisher.publish(activationRequestEvent);
    }

    public void onActivated(ActivationResponseEvent activationResponseEvent) {
        if (activationResponseEvent.getException() != null) {
            //todo handle exception
            log.error("Error -> {}", activationResponseEvent.getException().getMessage(), activationResponseEvent.getException());

        } else {
            OrchestrationEventContext oc = activationResponseEvent.getOrchestrationEventContext();
            requestInvocation(oc.getWorkflowExecution(), oc.getNodeExecution());
        }
    }


    private void requestInvocation(WorkflowExecution we, WorkflowNodeExecution ne) {
        InvocationRequestEvent invocationRequestEvent =
                InvocationRequestEvent.builder(this)
                        .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .orchestrationEventContext(we, ne)
                        .component(ne.getWorkflowNode().getComponent())
                        .context(ne.getStageContext())
                        .build();
        eventPublisher.publish(invocationRequestEvent);
    }

    public void onInvoked(InvocationResponseEvent invocationResponseEvent) {
        if (invocationResponseEvent.getException() != null) {
            //todo handle exception
            log.error(invocationResponseEvent.getException().getMessage(), invocationResponseEvent.getException());
            return;
        }
        log.info(invocationResponseEvent.getContext().toString());
        OrchestrationEventContext oc = invocationResponseEvent.getOrchestrationEventContext();
        oc.getNodeExecution().setStageContext(invocationResponseEvent.getContext());
        proceed(oc.getWorkflowExecution(), oc.getNodeExecution());
    }
}
