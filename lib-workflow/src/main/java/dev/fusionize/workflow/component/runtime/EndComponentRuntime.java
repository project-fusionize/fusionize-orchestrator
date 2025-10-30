package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;

public abstract class EndComponentRuntime extends ComponentRuntime {
    protected EndComponentRuntime(EventPublisher<Event> publisher) {
        super(publisher);
    }

    public abstract void finish(ComponentFinishedEvent onFinish);
}
