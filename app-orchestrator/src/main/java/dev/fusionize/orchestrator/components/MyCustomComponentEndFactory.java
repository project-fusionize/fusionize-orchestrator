package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(type = MyCustomComponentEnd.class, name = "end", description = "End component", actors = {
        Actor.SYSTEM })
public class MyCustomComponentEndFactory implements ComponentRuntimeFactory<MyCustomComponentEnd> {

    @Override
    public MyCustomComponentEnd create() {
        return new MyCustomComponentEnd();
    }

}
