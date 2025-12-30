package dev.fusionize.workflow.agent;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.registery.WorkflowComponentRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowToolService {
    private final WorkflowComponentRegistry workflowComponentRegistry;

    public WorkflowToolService(WorkflowComponentRegistry workflowComponentRegistry) {
        this.workflowComponentRegistry = workflowComponentRegistry;
    }

    @Tool(name = "listAvailableWorkflowComponents",
            description = "List all available workflow components")
    public List<WorkflowComponent> listAvailableWorkflowComponents() {
        return workflowComponentRegistry.getComponents();
    }

}
