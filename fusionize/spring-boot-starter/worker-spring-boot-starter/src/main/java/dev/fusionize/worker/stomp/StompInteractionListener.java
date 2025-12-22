package dev.fusionize.worker.stomp;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.listeners.InteractionListener;
import dev.fusionize.workflow.listeners.LogListener;
import dev.fusionize.workflow.WorkflowLog;
import org.springframework.stereotype.Component;

@Component
public class StompInteractionListener implements InteractionListener {
    private static final String SEND_INTERACTION_ENDPOINT = "/app/workflow/interaction";

    private final WorkerStompSessionHandler stompSessionHandler;

    public StompInteractionListener(WorkerStompSessionHandler stompSessionHandler) {
        this.stompSessionHandler = stompSessionHandler;
    }

    @Override
    public void onInteraction(WorkflowInteraction interaction) {
        stompSessionHandler.send(SEND_INTERACTION_ENDPOINT, interaction);
    }
}
