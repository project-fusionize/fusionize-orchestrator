package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.WorkflowInteraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowInteractionControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WorkflowInteractionController workflowInteractionController;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendInteractionToCorrectTopic() {
        // setup
        WorkflowInteraction interaction = WorkflowInteraction.create(
                "wf-1", "domain", "exec-1", "node-1", "key-1", "comp",
                "actor", WorkflowInteraction.InteractionType.MESSAGE,
                WorkflowInteraction.Visibility.EXTERNAL, "content");
        Message<WorkflowInteraction> interactionMessage = org.mockito.Mockito.mock(Message.class);
        when(interactionMessage.getPayload()).thenReturn(interaction);

        // expectation
        workflowInteractionController.convertAndSend(interactionMessage);

        // validation
        verify(messagingTemplate).convertAndSend("/topic/1.0.workflow-executions.wf-1.interaction", interaction);
    }
}
