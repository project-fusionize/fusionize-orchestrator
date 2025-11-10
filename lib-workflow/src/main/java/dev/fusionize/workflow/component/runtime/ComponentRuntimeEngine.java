package dev.fusionize.workflow.component.runtime;


import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.exceptions.ComponentMissmatchException;
import dev.fusionize.workflow.component.exceptions.ComponentNotFoundException;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.RuntimeEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ComponentRuntimeEngine {
    public static final String ERR_CODE_COMP_NOT_FOUND = "(wcre101) Runtime component not found";
    public static final String ERR_CODE_COMP_NOT_MATCH = "(wcre102) Runtime component type not match";

    private final ComponentRuntimeRegistry componentRuntimeRegistry;
    private final EventStore<Event> eventStore;

    public ComponentRuntimeEngine(ComponentRuntimeRegistry componentRuntimeRegistry,
                                  EventStore<Event> eventStore) {
        this.componentRuntimeRegistry = componentRuntimeRegistry;
        this.eventStore = eventStore;
    }

    private Optional<ComponentRuntime> getRuntimeComponent(OrchestrationEvent orchestrationEvent) {
        String component = orchestrationEvent.getComponent();
        ComponentRuntimeConfig componentConfig = orchestrationEvent.getOrchestrationEventContext()
                .getNodeExecution().getWorkflowNode().getComponentConfig();
        return componentRuntimeRegistry.get(component, componentConfig);
    }

    public ActivationResponseEvent activateComponent(ActivationRequestEvent activationRequestEvent){
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  getRuntimeComponent(activationRequestEvent);
        if(optionalWorkflowComponentRuntime.isEmpty()){
            ActivationResponseEvent responseEvent = ActivationResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, activationRequestEvent);
            responseEvent.setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND + " " + activationRequestEvent.getComponent()));
            return responseEvent;
        }

        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        runtime.canActivate(
                ComponentActivatedEvent.builder(runtime)
                        .correlationId(activationRequestEvent.getCorrelationId())
                        .causationId(activationRequestEvent.getEventId())
                        .component(activationRequestEvent.getComponent())
                        .context(activationRequestEvent.getContext())
                        .build()
               );

        return null;
    }

    public ActivationResponseEvent onComponentActivated(ComponentActivatedEvent activatedEvent){
        String causationId =  activatedEvent.getCausationId();
        Optional<Event> requestEvent = this.eventStore.findByEventId(causationId);
        if(requestEvent.isEmpty() || !(requestEvent.get() instanceof ActivationRequestEvent)){
            //todo handle error
            return null;
        }

        ActivationResponseEvent responseEvent = ActivationResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, (ActivationRequestEvent) requestEvent.get());
        responseEvent.setContext(activatedEvent.getContext());
        responseEvent.setException(activatedEvent.getException());

        return responseEvent;
    }

    public InvocationResponseEvent invokeComponent(InvocationRequestEvent invocationRequestEvent) {
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  getRuntimeComponent(invocationRequestEvent);
        if(optionalWorkflowComponentRuntime.isEmpty()){
            InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
            invocationResponseEvent.setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
            return invocationResponseEvent;
        }
        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        WorkflowNodeType nodeType = invocationRequestEvent.getOrchestrationEventContext()
                .getNodeExecution().getWorkflowNode().getType();
        switch (nodeType) {
            case START -> {
                if(!(runtime instanceof StartComponentRuntime)){
                    InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                            this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
                    invocationResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
                    return invocationResponseEvent;
                }
                ((StartComponentRuntime) runtime).start(
                        ComponentTriggeredEvent.builder(runtime)
                                .correlationId(invocationRequestEvent.getCorrelationId())
                                .causationId(invocationRequestEvent.getEventId())
                                .component(invocationRequestEvent.getComponent())
                                .context(invocationRequestEvent.getContext())
                                .build()
                );
            }
            case DECISION -> {
                if(!(runtime instanceof DecisionComponentRuntime)){
                    InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                            this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
                    invocationResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
                    return invocationResponseEvent;
                }
                ((DecisionComponentRuntime) runtime).decide(
                        ComponentFinishedEvent.builder(runtime)
                                .correlationId(invocationRequestEvent.getCorrelationId())
                                .causationId(invocationRequestEvent.getEventId())
                                .component(invocationRequestEvent.getComponent())
                                .context(invocationRequestEvent.getContext())
                                .build()
                );
            }
            case TASK -> {
                if(!(runtime instanceof TaskComponentRuntime)){
                    InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                            this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
                    invocationResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
                    return invocationResponseEvent;
                }
                ((TaskComponentRuntime) runtime).run(
                        ComponentFinishedEvent.builder(runtime)
                                .correlationId(invocationRequestEvent.getCorrelationId())
                                .causationId(invocationRequestEvent.getEventId())
                                .component(invocationRequestEvent.getComponent())
                                .context(invocationRequestEvent.getContext())
                                .build()
                );
            }
            case WAIT -> {
                if(!(runtime instanceof WaitComponentRuntime)){
                    InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                            this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
                    invocationResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
                    return invocationResponseEvent;
                }
                ((WaitComponentRuntime) runtime).wait(
                        ComponentTriggeredEvent.builder(runtime)
                                .correlationId(invocationRequestEvent.getCorrelationId())
                                .causationId(invocationRequestEvent.getEventId())
                                .component(invocationRequestEvent.getComponent())
                                .context(invocationRequestEvent.getContext())
                                .build()
                );
            }
            case END -> {
                if(!(runtime instanceof EndComponentRuntime)){
                    InvocationResponseEvent invocationResponseEvent = InvocationResponseEvent.from(
                            this, OrchestrationEvent.Origin.RUNTIME_ENGINE, invocationRequestEvent);
                    invocationResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
                    return invocationResponseEvent;
                }
                ((EndComponentRuntime) runtime).finish(
                        ComponentFinishedEvent.builder(runtime)
                                .correlationId(invocationRequestEvent.getCorrelationId())
                                .causationId(invocationRequestEvent.getEventId())
                                .component(invocationRequestEvent.getComponent())
                                .context(invocationRequestEvent.getContext())
                                .build()
                );
            }
        }

        return null;
    }

    public OrchestrationEvent onComponentEvent(RuntimeEvent event) {
        String causationId =  event.getCausationId();
        Optional<Event> requestEvent = this.eventStore.findByEventId(causationId);

        if(requestEvent.isEmpty() || !(requestEvent.get() instanceof InvocationRequestEvent)){
            //todo handle error
            return null;
        }
        InvocationResponseEvent responseEvent = InvocationResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, (InvocationRequestEvent) requestEvent.get());
        responseEvent.setContext(event.getContext());
        responseEvent.setException(event.getException());

        return responseEvent;
    }

}
