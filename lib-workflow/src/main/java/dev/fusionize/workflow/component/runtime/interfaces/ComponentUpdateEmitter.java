package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.context.WorkflowContext;

public interface ComponentUpdateEmitter {
    void success(WorkflowContext updatedContext);

    void failure(Exception ex);

    void log(String message);

    void log(String message, WorkflowLog.LogLevel level);
}
