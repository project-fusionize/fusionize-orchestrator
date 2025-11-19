package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.component.local.LocalComponentBundle;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Component
public class OrchestratorComponentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorComponentDispatcher.class);
    private final EventPublisher<Event> eventPublisher;
    private final List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles;

    public OrchestratorComponentDispatcher(EventPublisher<Event> eventPublisher,
                                      List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles) {
        this.eventPublisher = eventPublisher;
        this.localComponentBundles = localComponentBundles;
    }

    public void dispatchActivation(WorkflowExecution we, WorkflowNodeExecution ne,
                                   BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
                                   BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        Optional<LocalComponentBundle<?>> optionalBundle = localComponentBundles.stream().filter(
                b -> b.matches(ne.getWorkflowNode().getComponent())).findFirst();
        if (optionalBundle.isPresent()) {
            LocalComponentBundle<?> localComponentBundle = optionalBundle.get();
            LocalComponentRuntime localComponentRuntime = localComponentBundle.newInstance();
            requestLocalActivation(localComponentRuntime, we, ne, onSuccess, onFailure);
        } else {
            requestRemoteActivation(we, ne);
        }
    }

    public void dispatchInvocation(WorkflowExecution we, WorkflowNodeExecution ne,
                                   BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
                                   BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        Optional<LocalComponentBundle<?>> optionalBundle = localComponentBundles.stream().filter(
                b -> b.matches(ne.getWorkflowNode().getComponent())).findFirst();
        if (optionalBundle.isPresent()) {
            LocalComponentBundle<?> localComponentBundle = optionalBundle.get();
            LocalComponentRuntime localComponentRuntime = localComponentBundle.newInstance();
            requestLocalInvocation(localComponentRuntime, we, ne, onSuccess, onFailure);
        } else {
            requestRemoteInvocation(we, ne);
        }
    }

    private void requestLocalActivation(LocalComponentRuntime localComponentRuntime,
                                        WorkflowExecution we, WorkflowNodeExecution ne,
                                        BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
                                        BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        localComponentRuntime.configure(ne.getWorkflowNode().getComponentConfig());
        CompletableFuture.runAsync(() ->
                localComponentRuntime.canActivate(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                    @Override
                    public void success(WorkflowContext updatedContext) {
                        // For activation, success means we can proceed to invocation
                        // But the Orchestrator logic was: requestLocalActivation -> success -> requestLocalInvocation
                        // Here we should probably just call the onSuccess callback which will trigger the next step in Orchestrator
                        // WAIT: Orchestrator's requestLocalActivation called requestLocalInvocation on success.
                        // So if we want to decouple, Orchestrator should pass a callback that does requestInvocation?
                        // OR ComponentDispatcher handles the transition from Activation -> Invocation for local components?
                        // The interface implies dispatching *one* thing.
                        // Let's stick to the interface contract.
                        // If dispatchActivation succeeds locally, it calls onSuccess.
                        // The Orchestrator's onSuccess for activation should be to call dispatchInvocation.
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }
                })
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                onFailure.accept(new Exception(throwable), ne);
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

    private void requestLocalInvocation(LocalComponentRuntime localComponentRuntime,
                                        WorkflowExecution we, WorkflowNodeExecution ne,
                                        BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
                                        BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        // We need to re-configure because it might be a new instance if called separately
        localComponentRuntime.configure(ne.getWorkflowNode().getComponentConfig());
        
        CompletableFuture.runAsync(() ->
                localComponentRuntime.run(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                    @Override
                    public void success(WorkflowContext updatedContext) {
                        ne.setStageContext(updatedContext);
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }
                })
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                onFailure.accept(new Exception(throwable), ne);
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
}
