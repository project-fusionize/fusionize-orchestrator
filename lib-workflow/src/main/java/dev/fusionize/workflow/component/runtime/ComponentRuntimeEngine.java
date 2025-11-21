package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.component.exceptions.ComponentNotFoundException;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
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

@Service
public class ComponentRuntimeEngine {
    public static final String ERR_CODE_COMP_NOT_FOUND = "(wcre101) Runtime component not found";

    private final ComponentRuntimeRegistry componentRuntimeRegistry;
    private final EventPublisher<Event> eventPublisher;
    private final WorkflowLogger workflowLogger;

    public ComponentRuntimeEngine(ComponentRuntimeRegistry componentRuntimeRegistry,
            EventPublisher<Event> eventPublisher,
            WorkflowLogger workflowLogger) {
        this.componentRuntimeRegistry = componentRuntimeRegistry;
        this.eventPublisher = eventPublisher;
        this.workflowLogger = workflowLogger;
    }

    private Optional<ComponentRuntime> getRuntimeComponent(OrchestrationEvent orchestrationEvent) {
        String component = orchestrationEvent.getComponent();
        ComponentRuntimeConfig componentConfig = orchestrationEvent.getOrchestrationEventContext()
                .nodeExecution().getWorkflowNode().getComponentConfig();
        return componentRuntimeRegistry.get(component, componentConfig);
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
                activationRequestEvent.getContext(),
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
                                    oc.workflowExecution().getWorkflowExecutionId(),
                                    oc.nodeExecution().getWorkflowNodeId(),
                                    oc.nodeExecution().getWorkflowNode().getComponent(), level, message);
                        };
                    }

                })).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        ActivationResponseEvent responseEvent = supplier.get();
                        responseEvent.setException(
                                new InterruptedException("activation interrupted: " + throwable.getMessage()));
                        eventPublisher.publish(responseEvent);
                    }
                });

        return null;
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
                invocationRequestEvent.getContext(),
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
                                    oc.workflowExecution().getWorkflowExecutionId(),
                                    oc.nodeExecution().getWorkflowNodeId(),
                                    oc.nodeExecution().getWorkflowNode().getComponent(), level, message);
                        };
                    }
                })).whenComplete((result, throwable) -> {
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
