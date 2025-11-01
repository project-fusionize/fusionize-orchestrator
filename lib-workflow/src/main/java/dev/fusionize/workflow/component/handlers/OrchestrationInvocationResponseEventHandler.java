package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationInvocationResponseEventHandler implements EventHandler<InvocationResponseEvent> {
    private final Orchestrator orchestrator;

    public OrchestrationInvocationResponseEventHandler(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }


    @Override
    public boolean shouldHandle(InvocationResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(InvocationResponseEvent.class)
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(InvocationResponseEvent event) {
        orchestrator.onInvoked(event);
        return null;
    }

    @Override
    public Class<InvocationResponseEvent> getEventType() {
        return InvocationResponseEvent.class;
    }
}
