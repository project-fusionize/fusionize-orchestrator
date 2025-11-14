package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "End Component",
        description = "A component to end",
        type = MyCustomComponentEnd.class,
        compatible = WorkflowNodeType.END
)
public class MyCustomComponentEndFactory implements ComponentRuntimeFactory<MyCustomComponentEnd> {

    @Override
    public MyCustomComponentEnd create() {
        return new MyCustomComponentEnd();
    }

}
