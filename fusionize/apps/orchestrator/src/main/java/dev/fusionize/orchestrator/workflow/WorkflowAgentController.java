package dev.fusionize.orchestrator.workflow;

import dev.fusionize.Application;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.workflow.agent.UserRequest;
import dev.fusionize.workflow.agent.WorkflowAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(Application.API_PREFIX + "/workflow-agent")
public class WorkflowAgentController {
    private static final Logger log = LoggerFactory.getLogger(WorkflowAgentController.class);
    private final WorkflowAgentService workflowAgentService;

    public WorkflowAgentController(WorkflowAgentService workflowAgentService) {
        this.workflowAgentService = workflowAgentService;
    }

    @PostMapping("/prompt")
    public ServicePayload<String> processPrompt(@RequestBody UserRequest userRequest) throws ChatModelException {
        return new ServicePayload.Builder<String>()
                .response(new ServiceResponse.Builder<String>()
                        .status(200)
                        .message(workflowAgentService.process(userRequest))
                        .build())
                .build();
    }

}
