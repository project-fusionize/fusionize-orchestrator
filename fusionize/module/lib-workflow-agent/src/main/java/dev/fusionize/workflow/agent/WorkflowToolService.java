package dev.fusionize.workflow.agent;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.component.registery.WorkflowComponentRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowToolService {
    private final WorkflowComponentRegistry workflowComponentRegistry;
    private final List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories;


    public WorkflowToolService(WorkflowComponentRegistry workflowComponentRegistry,
                               List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories) {
        this.workflowComponentRegistry = workflowComponentRegistry;
        this.localComponentRuntimeFactories = localComponentRuntimeFactories;
    }

    @Tool(name = "listAvailableWorkflowComponents",
            description = "List all available workflow components")
    public List<WorkflowComponent> listAvailableWorkflowComponents() {
        List<WorkflowComponent> workflowComponents = new ArrayList<>(workflowComponentRegistry.getComponents());
        workflowComponents.addAll(localComponentRuntimeFactories.stream().map(LocalComponentRuntimeFactory::describe).toList());
        return workflowComponents;
    }


    @Tool(name = "listAvailableWorkflowNodeTypes",
            description = "List all available workflow node types with their definition")
    public Map<WorkflowNodeType, String> listAvailableWorkflowNodeTypes() {
        return Map.of(
                WorkflowNodeType.START, "node that starts the workflow, this node can be an event receiver that keeps running and trigger workflow execution on event",
                WorkflowNodeType.END, "node that ends the workflow",
                WorkflowNodeType.TASK, "node that run a task, such as running script, do calculation",
                WorkflowNodeType.WAIT, "node that waits for an event before continue",
                WorkflowNodeType.DECISION, "node that decide which path should workflow go"
        );
    }

    @Tool(name = "getExampleWorkflowYaml",
            description = "get an example loan-origination workflow definition in yaml")
    public String getExampleWorkflowYaml() {
        try (var inputStream = getClass().getResourceAsStream("/examples/loan-origination.workflow.yml")) {
            if (inputStream == null) {
                return "Error: Example workflow not found.";
            }
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            return "Error reading example workflow: " + e.getMessage();
        }
    }

}
