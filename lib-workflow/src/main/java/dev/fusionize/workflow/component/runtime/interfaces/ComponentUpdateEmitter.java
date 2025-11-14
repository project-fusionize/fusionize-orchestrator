package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.WorkflowContext;

public interface ComponentUpdateEmitter {
    void success(WorkflowContext updatedContext);
    void failure(Exception ex);
}
