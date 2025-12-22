package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.WorkflowInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static dev.fusionize.orchestrator.config.WebSocketConfig.TOPIC_BASE;

@Controller
public class WorkflowInteractionController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowInteractionController.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WorkflowInteractionController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        logger.debug("WorkflowInteractionController initialized and ready to handle /workflow/interaction");
    }

    @MessageMapping("/workflow/interaction")
    public void convertAndSend(Message<WorkflowInteraction> interactionMessage) {
        WorkflowInteraction interaction = interactionMessage.getPayload();
        messagingTemplate.convertAndSend(
                TOPIC_BASE + ".workflow-executions." + interaction.getWorkflowId() + ".interaction", interaction);
    }
}
