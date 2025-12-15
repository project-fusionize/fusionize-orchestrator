package dev.fusionize.orchestrator.workflow;

import dev.fusionize.Application;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Application.API_PREFIX + "/workflow")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowRepoRegistry workflowRepoRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRepoRegistry;
    private final WorkflowLogger workflowLogger;
    private final WorkflowDescriptor workflowDescriptor;

    public WorkflowController(WorkflowRepoRegistry workflowRepoRegistry,
                              WorkflowExecutionRepoRegistry workflowExecutionRepoRegistry,
                              WorkflowLogger workflowLogger) {
        this.workflowRepoRegistry = workflowRepoRegistry;
        this.workflowExecutionRepoRegistry = workflowExecutionRepoRegistry;
        this.workflowLogger = workflowLogger;
        this.workflowDescriptor = new WorkflowDescriptor();
    }

    @GetMapping
    public ServicePayload<List<Workflow>> getAll() {
        List<Workflow> workflows = workflowRepoRegistry.getAll();
        return new ServicePayload.Builder<List<Workflow>>()
                .response(new ServiceResponse.Builder<List<Workflow>>()
                        .status(200)
                        .message(workflows)
                        .build())
                .build();
    }

    @GetMapping("/{domain}")
    public ServicePayload<Workflow> get(@PathVariable String domain) {
        Workflow workflow = workflowRepoRegistry.getWorkflowByDomain(domain);
        if (workflow == null) {
            return new ServicePayload.Builder<Workflow>()
                    .response(new ServiceResponse.Builder<Workflow>()
                            .status(404)
                            .message(null)
                            .build())
                    .build();
        }
        return new ServicePayload.Builder<Workflow>()
                .response(new ServiceResponse.Builder<Workflow>()
                        .status(200)
                        .message(workflow)
                        .build())
                .build();
    }

    @GetMapping("/{workflowId}/executions")
    public ServicePayload<List<WorkflowExecution>> getWorkflowExecutions(@PathVariable String workflowId) {
        List<WorkflowExecution> executions = workflowExecutionRepoRegistry.getWorkflowExecutions(workflowId);
        return new ServicePayload.Builder<List<WorkflowExecution>>()
                .response(new ServiceResponse.Builder<List<WorkflowExecution>>()
                        .status(200)
                        .message(executions)
                        .build())
                .build();
    }

    @GetMapping("/{workflowId}/executions/{workflowExecutionId}/logs")
    public ServicePayload<List<WorkflowLog>> getWorkflowExecutionLogs(@PathVariable String workflowId,
                                                                      @PathVariable String workflowExecutionId) {
        List<WorkflowLog> logs = workflowLogger.getLogs(workflowExecutionId);
        return new ServicePayload.Builder<List<WorkflowLog>>()
                .response(new ServiceResponse.Builder<List<WorkflowLog>>()
                        .status(200)
                        .message(logs)
                        .build())
                .build();
    }

    @PostMapping("/register")
    public ServicePayload<Workflow> registerWorkflow(
            @RequestBody String workflowDefinition,
            @RequestParam(defaultValue = "json") String format) {
        Workflow workflow;
        try {
            if ("yaml".equalsIgnoreCase(format) || "yml".equalsIgnoreCase(format)) {
                workflow = workflowDescriptor.fromYamlDescription(workflowDefinition);
            } else {
                // Default to JSON
                workflow = workflowDescriptor.fromJsonDescription(workflowDefinition);
            }
        } catch (Exception e) {
            log.error("Failed to parse workflow definition", e);
            return new ServicePayload.Builder<Workflow>()
                    .response(new ServiceResponse.Builder<Workflow>()
                            .status(400)
                            .message(null) // or specific error message if applicable
                            .build())
                    .build();
        }

        Workflow saved = workflowRepoRegistry.register(workflow);

        return new ServicePayload.Builder<Workflow>()
                .response(new ServiceResponse.Builder<Workflow>()
                        .status(200)
                        .message(saved)
                        .build())
                .build();
    }
}
