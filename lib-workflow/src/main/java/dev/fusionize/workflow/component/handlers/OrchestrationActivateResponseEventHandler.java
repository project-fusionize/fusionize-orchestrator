package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationActivateResponseEventHandler implements EventHandler<ActivationResponseEvent> {
    private final Orchestrator orchestrator;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRegistry workflowExecutionRegistry;
    public OrchestrationActivateResponseEventHandler(Orchestrator orchestrator,
                                                     WorkflowRegistry workflowRegistry,
                                                     WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.orchestrator = orchestrator;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }


    @Override
    public boolean shouldHandle(ActivationResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ActivationResponseEvent.class.getCanonicalName())
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(ActivationResponseEvent event) {
        event.ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        orchestrator.onActivated(event);
        return null;
    }

    @Override
    public Class<ActivationResponseEvent> getEventType() {
        return ActivationResponseEvent.class;
    }
}
