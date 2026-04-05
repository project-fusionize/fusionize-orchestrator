package dev.fusionize.worker.stomp;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.WorkflowLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompListenersTest {

    @Mock
    private WorkerStompSessionHandler stompSessionHandler;

    @Test
    void shouldSendInteraction_toCorrectEndpoint() {
        // setup
        var listener = new StompInteractionListener(stompSessionHandler);
        WorkflowInteraction interaction = WorkflowInteraction.create(
                "wf-1", "test-domain", "exec-1",
                "node-1", "start", "test-component",
                "system", WorkflowInteraction.InteractionType.MESSAGE,
                WorkflowInteraction.Visibility.EXTERNAL, "test content"
        );

        // expectation
        listener.onInteraction(interaction);

        // validation
        verify(stompSessionHandler).send("/app/workflow/interaction", interaction);
    }

    @Test
    void shouldSendLog_toCorrectEndpoint() {
        // setup
        var listener = new StompLogListener(stompSessionHandler);
        WorkflowLog log = WorkflowLog.info(
                "wf-1", "test-domain", "exec-1",
                "node-1", "start", "test-component",
                "test log message"
        );

        // expectation
        listener.onLog(log);

        // validation
        verify(stompSessionHandler).send("/app/workflow/log", log);
    }
}
