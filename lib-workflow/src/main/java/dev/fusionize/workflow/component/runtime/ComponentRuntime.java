package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.RuntimeEvent;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;

public abstract class ComponentRuntime {
    private final EventPublisher<Event> publisher;

    protected ComponentRuntime(EventPublisher<Event> publisher) {
        this.publisher = publisher;
    }

    public abstract void configure(ComponentRuntimeConfig config);
    public abstract void canActivate(ComponentActivatedEvent onActivate);
    public final void publish(RuntimeEvent event) {
        publisher.publish(event.renew());
    }
}
