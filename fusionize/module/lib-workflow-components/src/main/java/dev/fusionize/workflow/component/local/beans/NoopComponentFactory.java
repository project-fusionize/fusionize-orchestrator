package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopComponentFactory implements LocalComponentRuntimeFactory<NoopComponent> {

    @Override
    public String getName() {
        return NoopComponent.NAME;
    }

    @Override
    public NoopComponent create() {
        return new NoopComponent();
    }
}