package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.component.local.LocalComponentBundle;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.context.WorkflowContextFactory;
import dev.fusionize.workflow.context.WorkflowContextUtility;
import dev.fusionize.workflow.context.WorkflowDecision;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private final EventPublisher<Event> eventPublisher;
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    private final List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles;

    public Orchestrator(EventPublisher<Event> publisher,
                        WorkflowRepoRegistry workflowRegistry,
                        WorkflowExecutionRepoRegistry workflowExecutionRegistry,
                        List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles) {
        this.eventPublisher = publisher;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
        this.localComponentBundles = localComponentBundles;
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

        if (ne.getWorkflowNode().getType().equals(WorkflowNodeType.START)) {
            we = we.renew();
            we.setStatus(WorkflowExecutionStatus.IN_PROGRESS);

            ne = we.getNodes().stream().filter(n -> n.getWorkflowNodeId().equals(originalExecutionNode.getWorkflowNodeId()))
                    .findFirst().orElse(ne.renew());
            ne.setState(WorkflowNodeExecutionState.DONE);
        }

        if (ne.getWorkflowNode().getType().equals(WorkflowNodeType.END)) {
            we.setStatus(WorkflowExecutionStatus.SUCCESS);
        }

        ne.getChildren().addAll(nodeExecutions);

        WorkflowNodeExecution finalNe = ne;
        if (we.getWorkflow().getNodes().stream().anyMatch(n -> n.getWorkflowNodeId().equals(finalNe.getWorkflowNodeId()))) {
            we.getNodes().removeIf(cne -> cne.getWorkflowNodeExecutionId().equals(finalNe.getWorkflowNodeExecutionId()));
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
        WorkflowDecision lastDecision = WorkflowContextUtility.getLatestDecisionForNode(ne.getStageContext(),
                ne.getWorkflowNode().getWorkflowNodeKey());
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
        Optional<LocalComponentBundle<?>> optionalBundle = localComponentBundles.stream().filter(
                b -> b.matches(ne.getWorkflowNode().getComponent())).findFirst();
        if (optionalBundle.isPresent()) {
            LocalComponentBundle<?> localComponentBundle = optionalBundle.get();
            LocalComponentRuntime localComponentRuntime = localComponentBundle.newInstance();
            requestLocalActivation(localComponentRuntime, we, ne);
        } else {
            requestRemoteActivation(we, ne);
        }
    }

    private void requestLocalActivation(LocalComponentRuntime localComponentRuntime,
                                        WorkflowExecution we, WorkflowNodeExecution ne) {
        localComponentRuntime.configure(ne.getWorkflowNode().getComponentConfig());
        CompletableFuture.runAsync(() ->
            localComponentRuntime.canActivate(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                @Override
                public void success(WorkflowContext updatedContext) {
                    requestLocalInvocation(localComponentRuntime, we, ne);
                }

                @Override
                public void failure(Exception ex) {
                    //todo handle exception
                    log.error("Error -> {}", ex.getMessage(), ex);
                }
            })
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                //todo handle throwable
                log.error("Error -> {}", throwable.getMessage(), throwable);
            }
        });

    }

    private void requestRemoteActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
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
            OrchestrationEvent.EventContext oc = activationResponseEvent.getOrchestrationEventContext();
            requestRemoteInvocation(oc.workflowExecution(), oc.nodeExecution());
        }
    }

    private void requestLocalInvocation(LocalComponentRuntime localComponentRuntime,
                                        WorkflowExecution we, WorkflowNodeExecution ne) {
        CompletableFuture.runAsync(() ->
                localComponentRuntime.run(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                    @Override
                    public void success(WorkflowContext updatedContext) {
                        ne.setStageContext(updatedContext);
                        proceed(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        //todo handle exception
                        log.error("Error -> {}", ex.getMessage(), ex);
                    }
                })
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                //todo handle throwable
                log.error("Error -> {}", throwable.getMessage(), throwable);
            }
        });

    }

    private void requestRemoteInvocation(WorkflowExecution we, WorkflowNodeExecution ne) {
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
        OrchestrationEvent.EventContext oc = invocationResponseEvent.getOrchestrationEventContext();
        oc.nodeExecution().setStageContext(invocationResponseEvent.getContext());
        proceed(oc.workflowExecution(), oc.nodeExecution());
    }
}
