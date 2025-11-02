package dev.fusionize.workflow.descriptor;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;

import java.util.*;
import java.util.stream.Collectors;

public class WorkflowTransformer {

    /**
     * Transforms a WorkflowDescription to a Workflow entity.
     *
     * @param workflowDescription the description to transform
     * @return the transformed Workflow entity
     */
    public Workflow toWorkflow(WorkflowDescription workflowDescription) {
        if (workflowDescription == null) {
            return null;
        }

        Workflow.Builder builder = Workflow.builder("")
                .withDomain(workflowDescription.getDomain())
                .withName(workflowDescription.getName())
                .withDescription(workflowDescription.getDescription())
                .withKey(workflowDescription.getKey())
                .withVersion(workflowDescription.getVersion())
                .withActive(workflowDescription.isActive());

        // Transform nodes from map to hierarchical structure
        List<WorkflowNode> nodes = transformNodes(workflowDescription.getNodes());
        builder.withNodes(nodes);

        return builder.build();
    }

    /**
     * Transforms a Workflow entity to a WorkflowDescription.
     *
     * @param workflow the workflow entity to transform
     * @return the transformed WorkflowDescription
     */
    public WorkflowDescription toWorkflowDescription(Workflow workflow) {
        if (workflow == null) {
            return null;
        }

        WorkflowDescription description = new WorkflowDescription();
        description.setName(workflow.getName());
        description.setDomain(workflow.getDomain());
        description.setKey(workflow.getKey());
        description.setDescription(workflow.getDescription());
        description.setVersion(workflow.getVersion());
        description.setActive(workflow.isActive());

        // Transform nodes from hierarchical structure to map
        Map<String, WorkflowNodeDescription> nodes = transformNodesToMap(workflow.getNodes());
        description.setNodes(nodes);

        return description;
    }

    /**
     * Transforms a map of node descriptions to a hierarchical list of WorkflowNodes.
     * Uses the "next" field in each description to build parent-child relationships.
     */
    private List<WorkflowNode> transformNodes(Map<String, WorkflowNodeDescription> nodeDescriptions) {
        if (nodeDescriptions == null || nodeDescriptions.isEmpty()) {
            return new ArrayList<>();
        }

        // First pass: create all nodes without children
        Map<String, WorkflowNode> nodeMap = getStringWorkflowNodeMap(nodeDescriptions);

        // Second pass: build parent-child relationships using "next" field
        for (Map.Entry<String, WorkflowNodeDescription> entry : nodeDescriptions.entrySet()) {
            String nodeKey = entry.getKey();
            WorkflowNodeDescription nodeDescription = entry.getValue();
            WorkflowNode currentNode = nodeMap.get(nodeKey);

            if (nodeDescription.getNext() != null && !nodeDescription.getNext().isEmpty()) {
                List<WorkflowNode> children = new ArrayList<>();
                for (String nextKey : nodeDescription.getNext()) {
                    WorkflowNode childNode = nodeMap.get(nextKey);
                    if (childNode != null) {
                        children.add(childNode);
                    }
                }
                currentNode.setChildren(children);
            }
        }

        // Find root nodes: nodes that are not referenced in any "next" field
        Set<String> referencedNodes = nodeDescriptions.values().stream()
                .filter(desc -> desc.getNext() != null)
                .flatMap(desc -> desc.getNext().stream())
                .collect(Collectors.toSet());

        return nodeDescriptions.keySet().stream()
                .filter(workflowNodeDescription -> !referencedNodes.contains(workflowNodeDescription))
                .map(nodeMap::get)
                .collect(Collectors.toList());
    }

    private Map<String, WorkflowNode> getStringWorkflowNodeMap(Map<String, WorkflowNodeDescription> nodeDescriptions) {
        WorkflowNodeTransformer nodeTransformer = new WorkflowNodeTransformer();
        Map<String, WorkflowNode> nodeMap = new HashMap<>();

        for (Map.Entry<String, WorkflowNodeDescription> entry : nodeDescriptions.entrySet()) {
            String nodeKey = entry.getKey();
            WorkflowNodeDescription nodeDescription = entry.getValue();
            WorkflowNode node = nodeTransformer.toWorkflowNode(nodeDescription, nodeKey);
            nodeMap.put(nodeKey, node);
        }
        return nodeMap;
    }

    /**
     * Transforms a hierarchical list of WorkflowNodes to a flat map of WorkflowNodeDescriptions.
     * The map key is the workflowNodeKey, and the "next" field contains the keys of child nodes.
     */
    private Map<String, WorkflowNodeDescription> transformNodesToMap(List<WorkflowNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, WorkflowNodeDescription> nodeMap = new HashMap<>();
        WorkflowNodeTransformer nodeTransformer = new WorkflowNodeTransformer();

        // Recursively transform all nodes and their children
        transformNodeRecursive(nodes, nodeMap, nodeTransformer);

        return nodeMap;
    }

    /**
     * Recursively transforms nodes and their children.
     */
    private void transformNodeRecursive(List<WorkflowNode> nodes, 
                                       Map<String, WorkflowNodeDescription> nodeMap,
                                       WorkflowNodeTransformer transformer) {
        if (nodes == null) {
            return;
        }

        for (WorkflowNode node : nodes) {
            WorkflowNodeDescription description = transformer.toWorkflowNodeDescription(node);
            nodeMap.put(node.getWorkflowNodeKey(), description);

            // Recursively process children
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                transformNodeRecursive(node.getChildren(), nodeMap, transformer);
            }
        }
    }
}

