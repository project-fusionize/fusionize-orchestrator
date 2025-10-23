package dev.fusionize.workflow.component;

/**
 * Factory interface for creating WorkflowComponentRuntime instances.
 * This allows components to be created dynamically when configuration is not yet available.
 */
@FunctionalInterface
public interface WorkflowComponentFactory {
    /**
     * Creates a new WorkflowComponentRuntime instance.
     * 
     * @return A new instance of WorkflowComponentRuntime
     */
    WorkflowComponentRuntime create();
}
