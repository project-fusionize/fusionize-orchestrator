package dev.fusionize.orchestrator.workflow;

import dev.fusionize.Application;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
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
    private final WorkflowDescriptor workflowDescriptor;

    public WorkflowController(WorkflowRepoRegistry workflowRepoRegistry) {
        this.workflowRepoRegistry = workflowRepoRegistry;
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
