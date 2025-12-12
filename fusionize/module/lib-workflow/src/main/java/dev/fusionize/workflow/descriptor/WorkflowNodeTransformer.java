package dev.fusionize.workflow.descriptor;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.component.ComponentConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkflowNodeTransformer {

    /**
     * Transforms a WorkflowNodeDescription to a WorkflowNode entity.
     *
     * @param nodeDescription the description to transform
     * @param workflowNodeKey the key for this node (used as workflowNodeKey)
     * @return the transformed WorkflowNode entity
     */
    public WorkflowNode toWorkflowNode(WorkflowNodeDescription nodeDescription, String workflowNodeKey) {
        if (nodeDescription == null) {
            return null;
        }

        String fullComponent = nodeDescription.getComponent();
        if (nodeDescription.getActor() != null && !nodeDescription.getActor().isEmpty()) {
            fullComponent = nodeDescription.getActor() + ":" + nodeDescription.getComponent();
        }

        WorkflowNode.Builder builder = WorkflowNode.builder()
                .type(nodeDescription.getType())
                .component(fullComponent)
                .workflowNodeKey(workflowNodeKey);

        // Transform config from Map to WorkflowComponentConfig
        if (nodeDescription.getConfig() != null && !nodeDescription.getConfig().isEmpty()) {
            ComponentConfig componentConfig = ComponentConfig.builder()
                    .withConfig(nodeDescription.getConfig())
                    .build();
            builder.componentConfig(componentConfig);
        }

        // Note: children are not set here - they are set by WorkflowTransformer
        // based on the "next" field relationships

        return builder.build();
    }

    /**
     * Transforms a WorkflowNode entity to a WorkflowNodeDescription.
     *
     * @param workflowNode the workflow node entity to transform
     * @return the transformed WorkflowNodeDescription
     */
    public WorkflowNodeDescription toWorkflowNodeDescription(WorkflowNode workflowNode) {
        if (workflowNode == null) {
            return null;
        }

        WorkflowNodeDescription description = new WorkflowNodeDescription();
        description.setType(workflowNode.getType());

        String fullComponent = workflowNode.getComponent();
        if (fullComponent != null && fullComponent.contains(":")) {
            String[] parts = fullComponent.split(":", 2);
            description.setActor(parts[0]);
            description.setComponent(parts[1]);
        } else {
            description.setComponent(fullComponent);
        }

        // Transform componentConfig from WorkflowComponentConfig to Map
        if (workflowNode.getComponentConfig() != null
                && workflowNode.getComponentConfig().getConfig() != null
                && !workflowNode.getComponentConfig().getConfig().isEmpty()) {
            Map<String, Object> configMap = new HashMap<>(workflowNode.getComponentConfig().getConfig());
            description.setConfig(configMap);
        } else {
            description.setConfig(new HashMap<>());
        }

        // Transform children to "next" field containing the keys of child nodes
        if (workflowNode.getChildren() != null && !workflowNode.getChildren().isEmpty()) {
            List<String> nextKeys = workflowNode.getChildren().stream()
                    .map(WorkflowNode::getWorkflowNodeKey)
                    .filter(key -> key != null && !key.isEmpty())
                    .collect(Collectors.toList());
            description.setNext(nextKeys);
        } else {
            description.setNext(new ArrayList<>());
        }

        return description;
    }
}
