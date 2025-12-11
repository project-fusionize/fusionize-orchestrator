package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.component.ComponentConfig;
import dev.fusionize.workflow.component.exceptions.ComponentNotFoundException;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextRuntimeData;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.ExecutorService;

@Service
public class ComponentRuntimeEngine {
    public static final String ERR_CODE_COMP_NOT_FOUND = "(wcre101) Runtime component not found";

    private final ComponentRuntimeRegistry componentRuntimeRegistry;
    private final EventPublisher<Event> eventPublisher;
    private final WorkflowLogger workflowLogger;
    private final ExecutorService executor;

    public ComponentRuntimeEngine(ComponentRuntimeRegistry componentRuntimeRegistry,
            EventPublisher<Event> eventPublisher,
            WorkflowLogger workflowLogger,
            @Qualifier("componentExecutor") ExecutorService executor) {
        this.componentRuntimeRegistry = componentRuntimeRegistry;
        this.eventPublisher = eventPublisher;
        this.workflowLogger = workflowLogger;
        this.executor = executor;
    }

    private Optional<ComponentRuntime> getRuntimeComponent(OrchestrationEvent orchestrationEvent) {
        if(orchestrationEvent == null
                || orchestrationEvent.getComponent() == null
                || orchestrationEvent.getOrchestrationEventContext()==null
                || orchestrationEvent.getOrchestrationEventContext().nodeExecution() == null
                || orchestrationEvent.getOrchestrationEventContext().nodeExecution().getWorkflowNode() == null
        ) {
            return Optional.empty();
        }
        String component = orchestrationEvent.getComponent();
        ComponentConfig componentConfig = orchestrationEvent.getOrchestrationEventContext()
                .nodeExecution().getWorkflowNode().getComponentConfig();
        return componentRuntimeRegistry.get(component, ComponentRuntimeConfig.from(componentConfig));
    }

    public ActivationResponseEvent activateComponent(ActivationRequestEvent activationRequestEvent) {
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime = getRuntimeComponent(activationRequestEvent);
        if (optionalWorkflowComponentRuntime.isEmpty()) {
            ActivationResponseEvent responseEvent = ActivationResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, activationRequestEvent);
            responseEvent.setException(new ComponentNotFoundException(
                    ERR_CODE_COMP_NOT_FOUND + " " + activationRequestEvent.getComponent()));
            return responseEvent;
        }

        Supplier<ActivationResponseEvent> supplier = () -> ActivationResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, activationRequestEvent);

        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        CompletableFuture.runAsync(() -> runtime.canActivate(
                getContext(activationRequestEvent),
                new ComponentUpdateEmitter() {
                    @Override
                    public void success(Context updatedContext) {
                        ActivationResponseEvent responseEvent = supplier.get();
                        responseEvent.setContext(updatedContext);
                        eventPublisher.publish(responseEvent);
                    }

                    @Override
                    public void failure(Exception ex) {
                        ActivationResponseEvent responseEvent = supplier.get();
                        responseEvent.setException(ex);
                        eventPublisher.publish(responseEvent);
                    }

                    @Override
                    public Logger logger() {
                        return (message, level, throwable) -> {
                            ActivationResponseEvent responseEvent = supplier.get();
                            var oc = responseEvent.getOrchestrationEventContext();
                            workflowLogger.log(oc.workflowExecution().getWorkflowId(),
                                    oc.workflowExecution().getWorkflow().getDomain(),
                                    oc.workflowExecution().getWorkflowExecutionId(),
                                    oc.nodeExecution().getWorkflowNodeId(),
                                    oc.nodeExecution().getWorkflowNode().getWorkflowNodeKey(),
                                    oc.nodeExecution().getWorkflowNode().getComponent(), level, message);
                        };
                    }

                }), executor).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        ActivationResponseEvent responseEvent = supplier.get();
                        responseEvent.setException(
                                new InterruptedException("activation interrupted: " + throwable.getMessage()));
                        eventPublisher.publish(responseEvent);
                    }
                });

        return null;
    }

    private Context getContext(OrchestrationEvent orchestrationEvent) {
        Context context = orchestrationEvent.getContext().renew();
        context.setRuntimeData(ContextRuntimeData.from(
                orchestrationEvent.getOrchestrationEventContext().workflowExecution(),
                orchestrationEvent.getOrchestrationEventContext().nodeExecution()));
        return context;
    }

    public InvocationResponseEvent invokeComponent(InvocationRequestEvent invocationRequestEvent) {
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime = getRuntimeComponent(invocationRequestEvent);
        if (optionalWorkflowComponentRuntime.isEmpty()) {
            InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
            invocationResponseEvent.setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
            return invocationResponseEvent;
        }
        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        Supplier<InvocationResponseEvent> supplier = () -> InvocationResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
        CompletableFuture.runAsync(() -> runtime.run(
                getContext(invocationRequestEvent),
                new ComponentUpdateEmitter() {
                    @Override
                    public void success(Context updatedContext) {
                        InvocationResponseEvent responseEvent = supplier.get();
                        responseEvent.setContext(updatedContext);
                        eventPublisher.publish(responseEvent);
                    }

                    @Override
                    public void failure(Exception ex) {
                        InvocationResponseEvent responseEvent = supplier.get();
                        responseEvent.setException(ex);
                        eventPublisher.publish(responseEvent);
                    }

                    @Override
                    public Logger logger() {
                        return (message, level, throwable) -> {
                            InvocationResponseEvent responseEvent = supplier.get();
                            var oc = responseEvent.getOrchestrationEventContext();
                            workflowLogger.log(oc.workflowExecution().getWorkflowId(),
                                    oc.workflowExecution().getWorkflow().getDomain(),
                                    oc.workflowExecution().getWorkflowExecutionId(),
                                    oc.nodeExecution().getWorkflowNodeId(),
                                    oc.nodeExecution().getWorkflowNode().getWorkflowNodeKey(),
                                    oc.nodeExecution().getWorkflowNode().getComponent(), level, message);
                        };
                    }
                }), executor).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        InvocationResponseEvent responseEvent = supplier.get();
                        responseEvent.setException(
                                new InterruptedException("component run interrupted: " + throwable.getMessage()));
                        eventPublisher.publish(responseEvent);
                    }
                });

        return null;
    }

}
