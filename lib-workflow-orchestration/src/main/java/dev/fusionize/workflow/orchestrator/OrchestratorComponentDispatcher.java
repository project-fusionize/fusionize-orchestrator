package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
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

import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.ExecutorService;

@Component
public class OrchestratorComponentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorComponentDispatcher.class);
    private final EventPublisher<Event> eventPublisher;
    private final List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories;
    private final WorkflowLogger workflowLogger;
    private final ExecutorService executor;

    public OrchestratorComponentDispatcher(EventPublisher<Event> eventPublisher,
            List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories,
            WorkflowLogRepoLogger workflowLogger,
            @Qualifier("componentExecutor") ExecutorService executor) {
        this.eventPublisher = eventPublisher;
        this.localComponentRuntimeFactories = localComponentRuntimeFactories;
        this.workflowLogger = workflowLogger;
        this.executor = executor;
    }

    private Optional<LocalComponentRuntimeFactory<?>> checkForLocalComponent(WorkflowNodeExecution ne) {
        String component = ne.getWorkflowNode().getComponent();
        if (component == null || component.isEmpty()) {
            return localComponentRuntimeFactories.stream().filter(
                    f -> f.getName().equalsIgnoreCase(NoopComponent.NAME)).findFirst();
        }
        return localComponentRuntimeFactories.stream().filter(
                f -> f.getName().equalsIgnoreCase(component)).findFirst();
    }

    public void dispatchActivation(WorkflowExecution we, WorkflowNodeExecution ne,
            BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
            BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        Optional<LocalComponentRuntimeFactory<?>> optionalLocalFactory = checkForLocalComponent(ne);
        if (optionalLocalFactory.isPresent()) {
            LocalComponentRuntimeFactory<?> localComponentBundle = optionalLocalFactory.get();
            LocalComponentRuntime localComponentRuntime = localComponentBundle.create();
            requestLocalActivation(localComponentRuntime, we, ne, onSuccess, onFailure);
        } else {
            requestRemoteActivation(we, ne);
        }
    }

    public void dispatchInvocation(WorkflowExecution we, WorkflowNodeExecution ne,
            BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
            BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        Optional<LocalComponentRuntimeFactory<?>> optionalLocalFactory = checkForLocalComponent(ne);
        if (optionalLocalFactory.isPresent()) {
            LocalComponentRuntimeFactory<?> localComponentBundle = optionalLocalFactory.get();
            LocalComponentRuntime localComponentRuntime = localComponentBundle.create();
            requestLocalInvocation(localComponentRuntime, we, ne, onSuccess, onFailure);
        } else {
            requestRemoteInvocation(we, ne);
        }
    }

    private void requestLocalActivation(LocalComponentRuntime localComponentRuntime,
            WorkflowExecution we, WorkflowNodeExecution ne,
            BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
            BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        localComponentRuntime.configure(ComponentRuntimeConfig.from(ne.getWorkflowNode().getComponentConfig()));
        CompletableFuture.runAsync(
                () -> localComponentRuntime.canActivate(getContext(we, ne), new ComponentUpdateEmitter() {
                    @Override
                    public void success(Context updatedContext) {
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }

                    @Override
                    public Logger logger() {
                        return (message, level, throwable) -> workflowLogger.log(
                                we.getWorkflowId(), we.getWorkflow().getDomain(), we.getWorkflowExecutionId(),
                                ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getWorkflowNodeKey(),
                                ne.getWorkflowNode().getComponent(), level, message);
                    }

                }), executor).whenComplete((result, throwable) -> {
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

    private Context getContext(WorkflowExecution we, WorkflowNodeExecution ne) {
        Context context = ne.getStageContext().renew();
        context.setRuntimeData(ContextRuntimeData.from(we, ne));
        return context;
    }

    private void requestLocalInvocation(LocalComponentRuntime localComponentRuntime,
            WorkflowExecution we, WorkflowNodeExecution ne,
            BiConsumer<WorkflowExecution, WorkflowNodeExecution> onSuccess,
            BiConsumer<Exception, WorkflowNodeExecution> onFailure) {
        localComponentRuntime.configure(ComponentRuntimeConfig.from(ne.getWorkflowNode().getComponentConfig()));
        CompletableFuture
                .runAsync(() -> localComponentRuntime.run(getContext(we, ne), new ComponentUpdateEmitter() {
                    @Override
                    public void success(Context updatedContext) {
                        ne.setStageContext(updatedContext);
                        onSuccess.accept(we, ne);
                    }

                    @Override
                    public void failure(Exception ex) {
                        onFailure.accept(ex, ne);
                    }

                    @Override
                    public Logger logger() {
                        return (message, level, throwable) -> workflowLogger.log(
                                we.getWorkflowId(), we.getWorkflow().getDomain(), we.getWorkflowExecutionId(),
                                ne.getWorkflowNodeId(),
                                ne.getWorkflowNode().getWorkflowNodeKey(),
                                ne.getWorkflowNode().getComponent(), level, message);
                    }
                }), executor).whenComplete((result, throwable) -> {
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
