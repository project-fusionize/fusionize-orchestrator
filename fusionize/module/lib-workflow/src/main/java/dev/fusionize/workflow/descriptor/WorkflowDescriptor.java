package dev.fusionize.workflow.descriptor;

import dev.fusionize.common.parser.JsonParser;
import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.common.parser.descriptor.JsonDescriptor;
import dev.fusionize.common.parser.descriptor.YamlDescriptor;
import dev.fusionize.workflow.Workflow;

public class WorkflowDescriptor implements YamlDescriptor<Workflow>, JsonDescriptor<Workflow> {
    private final YamlParser<WorkflowDescription> yamlParser = new YamlParser<>();
    private final JsonParser<WorkflowDescription> jsonParser = new JsonParser<>();


    @Override
    public Workflow fromJsonDescription(String json) {
        WorkflowDescription workflowDescription = jsonParser.fromJson(json, WorkflowDescription.class);
        return new WorkflowTransformer().toWorkflow(workflowDescription);
    }

    @Override
    public String toJsonDescription(Workflow model) {
        WorkflowDescription description = new WorkflowTransformer().toWorkflowDescription(model);
        return jsonParser.toJson(description, WorkflowDescription.class);
    }

    @Override
    public Workflow fromYamlDescription(String yaml) {
        WorkflowDescription workflowDescription = yamlParser.fromYaml(yaml, WorkflowDescription.class);
        return new WorkflowTransformer().toWorkflow(workflowDescription);
    }

    @Override
    public String toYamlDescription(Workflow model) {
        WorkflowDescription description = new WorkflowTransformer().toWorkflowDescription(model);
        return yamlParser.toYaml(description);
    }
}
