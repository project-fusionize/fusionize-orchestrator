package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;
import org.springframework.stereotype.Component;

@Component
public class ComponentFinishedEventHandler implements EventHandler<ComponentFinishedEvent> {
    private final ComponentRuntimeEngine componentRuntimeEngine;

    public ComponentFinishedEventHandler(ComponentRuntimeEngine componentRuntimeEngine) {
        this.componentRuntimeEngine = componentRuntimeEngine;
    }

    @Override
    public boolean shouldHandle(ComponentFinishedEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ComponentFinishedEvent.class.getCanonicalName());
    }

    @Override
    public Event handle(ComponentFinishedEvent event) {
        return componentRuntimeEngine.onComponentEvent(event);
    }

    @Override
    public Class<ComponentFinishedEvent> getEventType() {
        return ComponentFinishedEvent.class;
    }
}