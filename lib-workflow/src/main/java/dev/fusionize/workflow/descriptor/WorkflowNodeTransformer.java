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

        WorkflowNode.Builder builder = WorkflowNode.builder()
                .type(nodeDescription.getType())
                .component(nodeDescription.getComponent())
                .workflowNodeKey(workflowNodeKey);

        // Transform componentConfig from Map to WorkflowComponentConfig
        if (nodeDescription.getComponentConfig() != null && !nodeDescription.getComponentConfig().isEmpty()) {
            ComponentConfig componentConfig = ComponentConfig.builder()
                    .withConfig(nodeDescription.getComponentConfig())
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
        description.setComponent(workflowNode.getComponent());

        // Transform componentConfig from WorkflowComponentConfig to Map
        if (workflowNode.getComponentConfig() != null 
                && workflowNode.getComponentConfig().getConfig() != null
                && !workflowNode.getComponentConfig().getConfig().isEmpty()) {
            Map<String, Object> configMap = new HashMap<>(workflowNode.getComponentConfig().getConfig());
            description.setComponentConfig(configMap);
        } else {
            description.setComponentConfig(new HashMap<>());
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

