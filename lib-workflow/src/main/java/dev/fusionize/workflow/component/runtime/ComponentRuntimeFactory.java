package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;

/**
 * Factory interface for creating WorkflowComponentRuntime instances.
 * This allows components to be created dynamically when configuration is not yet available.
 */
@FunctionalInterface
public interface ComponentRuntimeFactory<T extends ComponentRuntime> {
    /**
     * Creates a new WorkflowComponentRuntime instance.
     * 
     * @return A new instance of WorkflowComponentRuntime
     */
    T create();
}
