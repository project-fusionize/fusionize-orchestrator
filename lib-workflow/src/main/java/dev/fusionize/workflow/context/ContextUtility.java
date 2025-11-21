package dev.fusionize.workflow.context;

import java.util.*;

public class ContextUtility {
    /**
     * Reconstructs a list of recursive WorkflowGraphNode
     * where parents are full object references instead of IDs.
     * Only leaf nodes (nodes with no children) are included in the result.
     *
     * @param context The Mongo-safe Context
     * @return List of leaf nodeId to reconstructed WorkflowGraphNode
     */
    static List<WorkflowGraphNodeRecursive> extractCurrentNodes(Context context) {
        if (context == null || context.getGraphNodes() == null) {
            return Collections.emptyList();
        }

        // Step 1: Create all nodes in the recursive model
        Map<String, WorkflowGraphNodeRecursive> nodeMap = new HashMap<>();
        for (WorkflowGraphNode node : context.getGraphNodes()) {
            nodeMap.put(node.getNode(), new WorkflowGraphNodeRecursive(
                    node.getNode(),
                    node.getState()));
        }

        // Step 2: Resolve parent references and populate children
        for (WorkflowGraphNode node : context.getGraphNodes()) {
            WorkflowGraphNodeRecursive recursiveNode = nodeMap.get(node.getNode());
            for (String parentId : node.getParents()) {
                WorkflowGraphNodeRecursive parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    recursiveNode.getParents().add(parentNode);
                    parentNode.getChildren().add(recursiveNode);
                }
            }
        }

        // Step 3: Filter only leaf nodes (nodes with no children)
        Map<String, WorkflowGraphNodeRecursive> leafNodes = new HashMap<>();
        for (Map.Entry<String, WorkflowGraphNodeRecursive> entry : nodeMap.entrySet()) {
            if (entry.getValue().getChildren().isEmpty()) {
                leafNodes.put(entry.getKey(), entry.getValue());
            }
        }

        return new ArrayList<>(leafNodes.values());
    }

    /**
     * Returns the latest WorkflowDecision for a given node key.
     * Traverses the decisions from end to start and returns the first match.
     *
     * @param context The Context
     * @param nodeKey The workflow node key to search for
     * @return Latest WorkflowDecision for the node, or null if not found
     */
    static WorkflowDecision getLatestDecisionForNode(Context context, String nodeKey) {
        if (context == null || context.getDecisions() == null) {
            return null;
        }

        List<WorkflowDecision> decisions = context.getDecisions();
        for (int i = decisions.size() - 1; i >= 0; i--) {
            WorkflowDecision decision = decisions.get(i);
            if (decision != null && nodeKey.equals(decision.getDecisionNode())) {
                return decision;
            }
        }
        return null;
    }

}
