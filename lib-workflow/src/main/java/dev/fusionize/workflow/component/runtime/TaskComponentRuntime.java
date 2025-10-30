package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;

public abstract class TaskComponentRuntime extends ComponentRuntime {
    protected TaskComponentRuntime(EventPublisher<Event> publisher) {
        super(publisher);
    }

    public abstract void run(ComponentFinishedEvent onFinish);

}
