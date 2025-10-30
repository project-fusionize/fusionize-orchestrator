package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivateRequestEvent;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationActivateRequestEventHandler implements EventHandler<ActivateRequestEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;

    public OrchestrationActivateRequestEventHandler(WorkflowComponentRuntimeEngine runtimeEngine) {
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    public boolean shouldHandle(ActivateRequestEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ActivateRequestEvent.class)
                && OrchestrationEvent.Origin.ORCHESTRATOR.equals(event.getOrigin());
    }

    @Override
    public Event handle(ActivateRequestEvent event) {
        return runtimeEngine.activateComponent(event);
    }

    @Override
    public Class<ActivateRequestEvent> getEventType() {
        return ActivateRequestEvent.class;
    }
}
