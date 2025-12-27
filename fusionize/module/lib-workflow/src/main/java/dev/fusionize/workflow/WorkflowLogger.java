package dev.fusionize.workflow;

import dev.fusionize.workflow.listeners.LogListener;

import java.util.List;

public interface WorkflowLogger {

    void log(String workflowId, String workflowDomain, String workflowExecutionId, String workflowNodeId,
            String nodeKey, String component,
            WorkflowLog.LogLevel level, String message);

    List<WorkflowLog> getLogs(String workflowExecutionId);

    default void addListener(LogListener listener) {}

    default void removeListener(LogListener listener) {}
}
