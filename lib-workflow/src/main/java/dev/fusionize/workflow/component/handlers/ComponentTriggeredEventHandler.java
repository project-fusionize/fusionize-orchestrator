package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.springframework.stereotype.Component;

@Component
public class ComponentTriggeredEventHandler implements EventHandler<ComponentTriggeredEvent> {
    private final ComponentRuntimeEngine componentRuntimeEngine;

    public ComponentTriggeredEventHandler(ComponentRuntimeEngine componentRuntimeEngine) {
        this.componentRuntimeEngine = componentRuntimeEngine;
    }

    @Override
    public boolean shouldHandle(ComponentTriggeredEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ComponentTriggeredEvent.class.getCanonicalName());

    }

    @Override
    public Event handle(ComponentTriggeredEvent event) {
        return componentRuntimeEngine.onComponentEvent(event);
    }

    @Override
    public Class<ComponentTriggeredEvent> getEventType() {
        return ComponentTriggeredEvent.class;
    }
}