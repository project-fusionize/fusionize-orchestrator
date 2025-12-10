package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class DelayComponentFactory implements LocalComponentRuntimeFactory<DelayComponent> {

    @Override
    public String getName() {
        return DelayComponent.NAME;
    }

    @Override
    public DelayComponent create() {
        return new DelayComponent();
    }
}
