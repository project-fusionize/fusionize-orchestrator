package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationInvocationRequestEventHandler implements EventHandler<InvocationRequestEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;

    public OrchestrationInvocationRequestEventHandler(WorkflowComponentRuntimeEngine runtimeEngine) {
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    public boolean shouldHandle(InvocationRequestEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(InvocationRequestEvent.class)
                && OrchestrationEvent.Origin.ORCHESTRATOR.equals(event.getOrigin());
    }

    @Override
    public Event handle(InvocationRequestEvent event) {
        return runtimeEngine.invokeComponent(event);
    }

    @Override
    public Class<InvocationRequestEvent> getEventType() {
        return InvocationRequestEvent.class;
    }
}

