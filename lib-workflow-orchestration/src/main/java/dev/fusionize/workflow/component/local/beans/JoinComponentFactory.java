package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class JoinComponentFactory implements LocalComponentRuntimeFactory<JoinComponent> {

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public JoinComponent create() {
        return new JoinComponent();
    }
}
