package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.StartResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationStartResponseEventHandler implements EventHandler<StartResponseEvent> {
    private final Orchestrator orchestrator;

    public OrchestrationStartResponseEventHandler(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }


    @Override
    public boolean shouldHandle(StartResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(StartResponseEvent.class)
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(StartResponseEvent event) {
        orchestrator.onStarted(event);
        return null;
    }

    @Override
    public Class<StartResponseEvent> getEventType() {
        return StartResponseEvent.class;
    }
}
