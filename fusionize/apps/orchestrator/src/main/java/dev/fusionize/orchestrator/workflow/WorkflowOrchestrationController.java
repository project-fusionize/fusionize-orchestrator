package dev.fusionize.orchestrator.workflow;

import dev.fusionize.Application;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Application.API_PREFIX + "/workflow-orchestration")
public class WorkflowOrchestrationController {
    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrationController.class);

    private final Orchestrator orchestrator;
    public WorkflowOrchestrationController( Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PutMapping("/replay/{workflowId}/{workflowExecutionId}/{workflowNodeExecutionId}")
    public ServicePayload<String> replayWorkflowNodeExecution(@PathVariable String workflowId,
                                                                      @PathVariable String workflowExecutionId,
                                                                      @PathVariable String workflowNodeExecutionId) {
        orchestrator.replayExecution(workflowId, workflowExecutionId, workflowNodeExecutionId);
        log.info("Replayed a workflow node execution with id {} and execution id {}", workflowNodeExecutionId, workflowExecutionId);
        return new ServicePayload.Builder<String>()
                .response(new ServiceResponse.Builder<String>()
                        .status(200)
                        .message("Replayed")
                        .build())
                .build();
    }
}
