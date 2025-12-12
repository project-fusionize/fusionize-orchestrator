package dev.fusionize.workflow.component.local;

public interface LocalComponentRuntimeFactory<T extends LocalComponentRuntime> {
    String getName();
    T create();
}
