package dev.fusionize.workflow.component.local;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;

public interface LocalComponentRuntimeFactory<T extends LocalComponentRuntime> extends ComponentRuntimeFactory<T> {
    WorkflowComponent describe();
    String getName();
    T create();
}
