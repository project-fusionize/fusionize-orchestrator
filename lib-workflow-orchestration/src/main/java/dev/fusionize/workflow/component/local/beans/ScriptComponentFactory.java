package dev.fusionize.workflow.component.local.beans;


import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class ScriptComponentFactory implements LocalComponentRuntimeFactory<ScriptComponent> {
    @Override
    public String getName() {
        return "script";
    }

    @Override
    public ScriptComponent create() {
        return new ScriptComponent();
    }
}

