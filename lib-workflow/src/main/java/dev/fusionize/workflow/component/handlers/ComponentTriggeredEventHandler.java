package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.springframework.stereotype.Component;

@Component
public class ComponentTriggeredEventHandler implements EventHandler<ComponentTriggeredEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;

    public ComponentTriggeredEventHandler(WorkflowComponentRuntimeEngine runtimeEngine) {
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    public boolean shouldHandle(ComponentTriggeredEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ComponentTriggeredEvent.class);

    }

    @Override
    public Event handle(ComponentTriggeredEvent event) {
        return runtimeEngine.onComponentEvent(event);
    }

    @Override
    public Class<ComponentTriggeredEvent> getEventType() {
        return ComponentTriggeredEvent.class;
    }
}