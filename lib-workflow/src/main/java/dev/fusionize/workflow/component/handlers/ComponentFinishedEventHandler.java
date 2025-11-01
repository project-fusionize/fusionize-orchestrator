package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;
import org.springframework.stereotype.Component;

@Component
public class ComponentFinishedEventHandler implements EventHandler<ComponentFinishedEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;

    public ComponentFinishedEventHandler(WorkflowComponentRuntimeEngine runtimeEngine) {
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    public boolean shouldHandle(ComponentFinishedEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ComponentFinishedEvent.class);
    }

    @Override
    public Event handle(ComponentFinishedEvent event) {
        return runtimeEngine.onComponentEvent(event);
    }

    @Override
    public Class<ComponentFinishedEvent> getEventType() {
        return ComponentFinishedEvent.class;
    }
}