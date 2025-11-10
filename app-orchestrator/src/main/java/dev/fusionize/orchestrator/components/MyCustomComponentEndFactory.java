package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RuntimeComponentDefinition(
        name = "End Component",
        description = "A component to end",
        type = MyCustomComponentEnd.class,
        compatible = WorkflowNodeType.END
)
public class MyCustomComponentEndFactory implements ComponentRuntimeFactory<MyCustomComponentEnd> {
    private final EventPublisher<Event> eventPublisher;

    public MyCustomComponentEndFactory(EventPublisher<Event> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MyCustomComponentEnd create() {
        return new MyCustomComponentEnd(eventPublisher);
    }

}
