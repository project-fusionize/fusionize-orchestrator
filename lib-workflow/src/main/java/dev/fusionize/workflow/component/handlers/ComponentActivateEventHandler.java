package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import org.springframework.stereotype.Component;

@Component
public class ComponentActivateEventHandler implements EventHandler<ComponentActivatedEvent> {
    private final ComponentRuntimeEngine componentRuntimeEngine;

    public ComponentActivateEventHandler(ComponentRuntimeEngine componentRuntimeEngine) {
        this.componentRuntimeEngine = componentRuntimeEngine;
    }

    @Override
    public boolean shouldHandle(ComponentActivatedEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ComponentActivatedEvent.class.getCanonicalName());
    }

    @Override
    public Event handle(ComponentActivatedEvent event) {
        return componentRuntimeEngine.onComponentActivated(event);
    }

    @Override
    public Class<ComponentActivatedEvent> getEventType() {
        return ComponentActivatedEvent.class;
    }
}
