package dev.fusionize.workflow.listeners;

import dev.fusionize.workflow.WorkflowLog;

/**
 * Functional interface for listening to workflow logs.
 */
public interface LogListener {
    void onLog(WorkflowLog log);
}
