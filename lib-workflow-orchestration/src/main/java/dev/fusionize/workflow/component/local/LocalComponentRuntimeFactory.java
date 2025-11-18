package dev.fusionize.workflow.component.local;

/**
 * Factory interface for creating WorkflowComponentRuntime instances.
 * This allows components to be created dynamically when configuration is not yet available.
 */
@FunctionalInterface
public interface LocalComponentRuntimeFactory<T extends LocalComponentRuntime> {
    /**
     * Creates a new WorkflowComponentRuntime instance.
     * 
     * @return A new instance of WorkflowComponentRuntime
     */
    T create();
}
