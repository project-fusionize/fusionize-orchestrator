package dev.fusionize.workflow.listeners;

import dev.fusionize.workflow.WorkflowInteraction;

/**
 * Functional interface for listening to workflow logs.
 */
public interface InteractionListener {
    void onInteraction(WorkflowInteraction log);
}
