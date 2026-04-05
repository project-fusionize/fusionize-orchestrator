package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.WorkflowLog;
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
class WorkflowLogControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WorkflowLogController workflowLogController;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendLogToCorrectTopic() {
        // setup
        WorkflowLog log = WorkflowLog.info("wf-1", "domain", "exec-1", "node-1", "key-1", "comp", "message");
        Message<WorkflowLog> logMessage = org.mockito.Mockito.mock(Message.class);
        when(logMessage.getPayload()).thenReturn(log);

        // expectation
        workflowLogController.convertAndSend(logMessage);

        // validation
        verify(messagingTemplate).convertAndSend("/topic/1.0.workflow-executions.wf-1.logs", log);
    }
}
