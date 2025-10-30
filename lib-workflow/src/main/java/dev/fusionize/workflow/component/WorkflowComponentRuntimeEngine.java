package dev.fusionize.workflow.component;


import dev.fusionize.workflow.component.exceptions.ComponentMissmatchException;
import dev.fusionize.workflow.component.exceptions.ComponentNotFoundException;
import dev.fusionize.workflow.component.runtime.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.StartComponentRuntime;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivateRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivateResponseEvent;
import dev.fusionize.workflow.events.orchestration.StartRequestEvent;
import dev.fusionize.workflow.events.orchestration.StartResponseEvent;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkflowComponentRuntimeEngine {
    public static final String ERR_CODE_COMP_NOT_FOUND = "(wcre101) Runtime component not found";
    public static final String ERR_CODE_COMP_NOT_MATCH = "(wcre102) Runtime component type not match";

    private final WorkflowComponentRegistry workflowComponentRegistry;
    private final EventStore<Event> eventStore;

    public WorkflowComponentRuntimeEngine(WorkflowComponentRegistry workflowComponentRegistry,
                                          EventStore<Event> eventStore) {
        this.workflowComponentRegistry = workflowComponentRegistry;
        this.eventStore = eventStore;
    }

    private Optional<ComponentRuntime> getRuntimeComponent(OrchestrationEvent orchestrationEvent) {
        String component = orchestrationEvent.getComponent();
        WorkflowComponentConfig componentConfig = orchestrationEvent.getOrchestrationEventContext()
                .getNodeExecution().getWorkflowNode().getComponentConfig();
        return workflowComponentRegistry.get(component, componentConfig);
    }

    public ActivateResponseEvent activateComponent(ActivateRequestEvent activateRequestEvent){
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  getRuntimeComponent(activateRequestEvent);
        if(optionalWorkflowComponentRuntime.isEmpty()){
            ActivateResponseEvent responseEvent = ActivateResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, activateRequestEvent);
            responseEvent.setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
            return responseEvent;
        }

        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        runtime.canActivate(
                ComponentActivatedEvent.builder(runtime)
                        .correlationId(activateRequestEvent.getCorrelationId())
                        .causationId(activateRequestEvent.getEventId())
                        .component(activateRequestEvent.getComponent())
                        .context(activateRequestEvent.getContext())
                        .build()
               );

        return null;
    }

    public ActivateResponseEvent onComponentActivated(ComponentActivatedEvent activatedEvent){
        String causationId =  activatedEvent.getCausationId();
        Optional<Event> requestEvent = this.eventStore.findByEventId(causationId);
        if(requestEvent.isEmpty() || !(requestEvent.get() instanceof ActivateRequestEvent)){
            //todo handle error
            return null;
        }

        ActivateResponseEvent responseEvent = ActivateResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, (ActivateRequestEvent) requestEvent.get());
        responseEvent.setContext(activatedEvent.getContext());
        responseEvent.setException(activatedEvent.getException());

        return responseEvent;
    }

    public StartResponseEvent startComponent(StartRequestEvent startRequestEvent) {
        Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  getRuntimeComponent(startRequestEvent);
        if(optionalWorkflowComponentRuntime.isEmpty()){
            StartResponseEvent startResponseEvent = StartResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, startRequestEvent);
            startResponseEvent.setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
            return startResponseEvent;
        }
        ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
        if(!(runtime instanceof StartComponentRuntime)){
            StartResponseEvent startResponseEvent = StartResponseEvent.from(
                    this, OrchestrationEvent.Origin.RUNTIME_ENGINE, startRequestEvent);
            startResponseEvent.setException(new ComponentMissmatchException(ERR_CODE_COMP_NOT_MATCH));
            return startResponseEvent;
        }
        ((StartComponentRuntime) runtime).start(
                ComponentTriggeredEvent.builder(runtime)
                        .correlationId(startRequestEvent.getCorrelationId())
                        .causationId(startRequestEvent.getEventId())
                        .component(startRequestEvent.getComponent())
                        .context(startRequestEvent.getContext())
                        .build()
        );
        return null;
    }

    public OrchestrationEvent onComponentTriggered(ComponentTriggeredEvent triggeredEvent) {
        String causationId =  triggeredEvent.getCausationId();
        Optional<Event> requestEvent = this.eventStore.findByEventId(causationId);

        //todo fix: assumption of StartRequestEvent may not always true
        if(requestEvent.isEmpty() || !(requestEvent.get() instanceof StartRequestEvent)){
            //todo handle error
            return null;
        }
        StartResponseEvent responseEvent = StartResponseEvent.from(
                this, OrchestrationEvent.Origin.RUNTIME_ENGINE, (StartRequestEvent) requestEvent.get());
        responseEvent.setContext(triggeredEvent.getContext());
        responseEvent.setException(triggeredEvent.getException());

        return responseEvent;
    }

}
