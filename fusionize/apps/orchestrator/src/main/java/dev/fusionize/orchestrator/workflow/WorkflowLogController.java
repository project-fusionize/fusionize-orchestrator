package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.WorkflowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static dev.fusionize.orchestrator.config.WebSocketConfig.TOPIC_BASE;

@Controller
public class WorkflowLogController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowLogController.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WorkflowLogController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        logger.debug("WorkflowLogController initialized and ready to handle /workflow/log");
    }

    @MessageMapping("/workflow/log")
    public void convertAndSend(Message<WorkflowLog> logMessage) {
        WorkflowLog log = logMessage.getPayload();
        messagingTemplate.convertAndSend(
                TOPIC_BASE + ".workflow-executions." + log.getWorkflowId() + ".logs", log);
    }
}
