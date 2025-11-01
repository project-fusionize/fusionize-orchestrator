package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationActivateResponseEventHandler implements EventHandler<ActivationResponseEvent> {
    private final Orchestrator orchestrator;

    public OrchestrationActivateResponseEventHandler(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }


    @Override
    public boolean shouldHandle(ActivationResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ActivationResponseEvent.class)
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(ActivationResponseEvent event) {
        orchestrator.onActivated(event);
        return null;
    }

    @Override
    public Class<ActivationResponseEvent> getEventType() {
        return ActivationResponseEvent.class;
    }
}
