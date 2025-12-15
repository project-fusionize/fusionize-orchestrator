package dev.fusionize.workflow;

/**
 * Functional interface for listening to workflow logs.
 */
public interface LogListener {
    void onLog(WorkflowLog log);
}
