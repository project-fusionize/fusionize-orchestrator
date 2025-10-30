package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.StartRequestEvent;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationStartRequestEventHandler  implements EventHandler<StartRequestEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;

    public OrchestrationStartRequestEventHandler(WorkflowComponentRuntimeEngine runtimeEngine) {
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    public boolean shouldHandle(StartRequestEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(StartRequestEvent.class)
                && OrchestrationEvent.Origin.ORCHESTRATOR.equals(event.getOrigin());
    }

    @Override
    public Event handle(StartRequestEvent event) {
        return runtimeEngine.startComponent(event);
    }

    @Override
    public Class<StartRequestEvent> getEventType() {
        return StartRequestEvent.class;
    }
}

