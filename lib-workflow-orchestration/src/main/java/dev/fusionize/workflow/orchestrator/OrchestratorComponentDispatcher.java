package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.WorkflowLogger;
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
import dev.fusionize.workflow.logging.WorkflowLogRepoLogger;
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
    private final WorkflowLogger workflowLogger;

    public OrchestratorComponentDispatcher(EventPublisher<Event> eventPublisher,
            List<LocalComponentBundle<? extends LocalComponentRuntime>> localComponentBundles,
            WorkflowLogRepoLogger workflowLogger) {
        this.eventPublisher = eventPublisher;
        this.localComponentBundles = localComponentBundles;
        this.workflowLogger = workflowLogger;
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
        CompletableFuture.runAsync(
                () -> localComponentRuntime.canActivate(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                    @Override
                    public void success(WorkflowContext updatedContext) {
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }

                    @Override
                    public void log(String message) {
                        workflowLogger.log(we.getWorkflowId(), we.getWorkflowExecutionId(), ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getComponent(), message);
                    }

                    @Override
                    public void log(String message, WorkflowLog.LogLevel level) {
                        workflowLogger.log(we.getWorkflowId(), we.getWorkflowExecutionId(), ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getComponent(), level, message);
                    }
                })).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        onFailure.accept(new Exception(throwable), ne);
                    }
                });
    }

    private void requestRemoteActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
        ActivationRequestEvent activationRequestEvent = ActivationRequestEvent.builder(this)
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
        localComponentRuntime.configure(ne.getWorkflowNode().getComponentConfig());
        CompletableFuture
                .runAsync(() -> localComponentRuntime.run(ne.getStageContext().renew(), new ComponentUpdateEmitter() {
                    @Override
                    public void success(WorkflowContext updatedContext) {
                        ne.setStageContext(updatedContext);
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }

                    @Override
                    public void log(String message) {
                        workflowLogger.log(we.getWorkflowId(), we.getWorkflowExecutionId(), ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getComponent(), message);
                    }

                    @Override
                    public void log(String message, WorkflowLog.LogLevel level) {
                        workflowLogger.log(we.getWorkflowId(), we.getWorkflowExecutionId(), ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getComponent(), level, message);
                    }
                })).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        onFailure.accept(new Exception(throwable), ne);
                    }
                });
    }

    private void requestRemoteInvocation(WorkflowExecution we, WorkflowNodeExecution ne) {
        InvocationRequestEvent invocationRequestEvent = InvocationRequestEvent.builder(this)
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
