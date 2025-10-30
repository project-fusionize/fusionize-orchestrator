package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;

public abstract class WaitComponentRuntime extends ComponentRuntime {
    protected WaitComponentRuntime(EventPublisher<Event> publisher) {
        super(publisher);
    }

    public abstract void wait(ComponentTriggeredEvent onTriggered);
}
