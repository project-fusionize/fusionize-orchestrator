package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.springframework.stereotype.Component;

@Component
public class ForkComponentFactory implements LocalComponentRuntimeFactory<ForkComponent> {
    @Override
    public String getName() {
        return "fork";
    }

    @Override
    public ForkComponent create() {
        return new ForkComponent();
    }
}
