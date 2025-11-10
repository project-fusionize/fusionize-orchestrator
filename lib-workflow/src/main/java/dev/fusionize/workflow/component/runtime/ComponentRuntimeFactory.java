package dev.fusionize.workflow.component.runtime;

/**
 * Factory interface for creating WorkflowComponentRuntime instances.
 * This allows components to be created dynamically when configuration is not yet available.
 */
@FunctionalInterface
public interface ComponentRuntimeFactory {
    /**
     * Creates a new WorkflowComponentRuntime instance.
     * 
     * @return A new instance of WorkflowComponentRuntime
     */
    ComponentRuntime create();
}
