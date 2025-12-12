package dev.fusionize.workflow.context;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.WorkflowNodeType;

import java.util.HashMap;
import java.util.Map;

public class ContextFactory {
    public static Context empty() {
        return new Context();
    }

    public static Context from(WorkflowNodeExecution lastExecution, WorkflowNode nextNode) {
        Context context = new Context();
        // Carry existing context forward (deep copy)
        if (lastExecution.getStageContext() != null) {
            context = lastExecution.getStageContext().renew();
        }

        // 1. Update graph node for the new workflow node
        if (nextNode != null) {
            addOrUpdateGraphNode(context, lastExecution, nextNode);
        }

        // 2. If next node is a decision, add decision metadata
        if (nextNode != null && WorkflowNodeType.DECISION.equals(nextNode.getType())) {
            addDecision(context, nextNode);
        }
        return context;
    }

    private static void addOrUpdateGraphNode(
            Context context,
            WorkflowNodeExecution lastExecution,
            WorkflowNode nextNode) {
        String nextNodeKey = nextNode.getWorkflowNodeKey();

        // Find existing graph node for this workflow node key (if exists)
        WorkflowGraphNode graphNode = context.getGraphNodes().stream()
                .filter(n -> nextNodeKey.equals(n.getNode()))
                .findFirst()
                .orElse(null);

        // Create new graph node if it doesn't exist yet
        if (graphNode == null) {
            graphNode = new WorkflowGraphNode();
            graphNode.setNode(nextNodeKey);
            context.getGraphNodes().add(graphNode);
        }

        // Set/update state
        graphNode.setState(WorkflowNodeExecutionState.IDLE);

        // Add parent if a previous node exists
        if (lastExecution != null && lastExecution.getWorkflowNode() != null) {
            String parentNodeKey = lastExecution.getWorkflowNode().getWorkflowNodeKey();
            if (!graphNode.getParents().contains(parentNodeKey)) {
                graphNode.getParents().add(parentNodeKey);
            }
        }
    }

    private static void addDecision(Context context, WorkflowNode nextNode) {
        WorkflowDecision workflowDecision = new WorkflowDecision();
        workflowDecision.setDecisionNode(nextNode.getWorkflowNodeKey());
        Map<String, Boolean> options = new HashMap<>();
        nextNode.getChildren().forEach(cn -> options.put(cn.getWorkflowNodeKey(), false));
        workflowDecision.setOptionNodes(options);
        context.getDecisions().add(workflowDecision);
    }
}
