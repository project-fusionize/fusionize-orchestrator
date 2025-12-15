package dev.fusionize.workflow.component.local;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;

public interface LocalComponentRuntimeFactory<T extends LocalComponentRuntime> extends ComponentRuntimeFactory<T> {
    String getName();
    T create();
}
