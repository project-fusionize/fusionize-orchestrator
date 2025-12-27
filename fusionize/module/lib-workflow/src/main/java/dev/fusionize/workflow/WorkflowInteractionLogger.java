package dev.fusionize.workflow;

import dev.fusionize.workflow.listeners.InteractionListener;

import java.util.List;

public interface WorkflowInteractionLogger {

    void log(String workflowId, String workflowDomain, String workflowExecutionId, String workflowNodeId,
             String nodeKey, String component, String actor,
             WorkflowInteraction.InteractionType type, WorkflowInteraction.Visibility visibility, Object content);

    List<WorkflowInteraction> getInteractions(String workflowExecutionId);

    default void addListener(InteractionListener listener) {
    }

    default void removeListener(InteractionListener listener) {
    }

}
