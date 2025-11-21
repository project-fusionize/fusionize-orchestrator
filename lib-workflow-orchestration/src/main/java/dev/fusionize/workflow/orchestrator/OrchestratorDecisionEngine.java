package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.context.WorkflowContextUtility;
import dev.fusionize.workflow.context.WorkflowDecision;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrchestratorDecisionEngine {

    public List<WorkflowNode> determineNextNodes(WorkflowNodeExecution ne) {
        List<WorkflowNode> allChildren = ne.getWorkflowNode().getChildren();
        if (ne.getStageContext().getDecisions().isEmpty()) {
            return allChildren;
        }
        if (!WorkflowNodeType.DECISION.equals(ne.getWorkflowNode().getType())) {
            return allChildren;
        }
        WorkflowDecision lastDecision = WorkflowContextUtility.getLatestDecisionForNode(ne.getStageContext(),
                ne.getWorkflowNode().getWorkflowNodeKey());
        if (lastDecision.getDecisionNode() == null
                || ne.getWorkflowNode().getWorkflowNodeKey() == null) {
            return new ArrayList<>();
        }
        if (!lastDecision.getDecisionNode().equals(ne.getWorkflowNode().getWorkflowNodeKey())) {
            return new ArrayList<>();
        }
        return allChildren.stream().filter(n -> n.getWorkflowNodeKey() != null)
                .filter(n -> lastDecision.getOptionNodes().get(n.getWorkflowNodeKey())).toList();
    }
}
