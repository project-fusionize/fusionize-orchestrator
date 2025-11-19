package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.context.WorkflowContextFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrchestratorWorkflowNavigator {

    private final OrchestratorDecisionEngine decisionEngine;

    public OrchestratorWorkflowNavigator(OrchestratorDecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    public List<WorkflowNodeExecution> navigate(WorkflowExecution we, WorkflowNodeExecution ne) {
        WorkflowNodeExecution originalExecutionNode = ne;
        ne.setState(WorkflowNodeExecutionState.DONE);
        List<WorkflowNodeExecution> nodeExecutions = decisionEngine.determineNextNodes(ne).stream()
                .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.from(originalExecutionNode, n)))
                .toList();

        if (ne.getWorkflowNode().getType().equals(WorkflowNodeType.START)) {
            we = we.renew();
            we.setStatus(WorkflowExecutionStatus.IN_PROGRESS);

            // Find the existing execution for the start node in the renewed list, or use the renewed one
            // The logic in Orchestrator was:
            // ne = we.getNodes().stream().filter(n -> n.getWorkflowNodeId().equals(originalExecutionNode.getWorkflowNodeId()))
            //         .findFirst().orElse(ne.renew());
            // But wait, 'we' is already renewed. 'we.getNodes()' contains renewed nodes.
            // 'ne' passed to this method is the OLD execution (before renewal).
            // So we need to find the corresponding NEW execution in 'we'.
            
            WorkflowNodeExecution renewedNe = we.getNodes().stream()
                    .filter(n -> n.getWorkflowNodeId().equals(originalExecutionNode.getWorkflowNodeId()))
                    .findFirst()
                    .orElse(ne.renew()); // Fallback, though it should be there if renew() works correctly
            
            renewedNe.setState(WorkflowNodeExecutionState.DONE);
            
            // We need to return the *new* children executions, but linked to the *new* parent?
            // The 'nodeExecutions' created above are linked to 'originalExecutionNode' context.
            // If we renewed the workflow, we should probably link them to the 'renewedNe' context?
            // The original logic didn't seem to re-link children to the new parent explicitly, 
            // but it did add them to 'ne.getChildren()'.
            
            // Let's stick to the exact logic from Orchestrator for now to avoid regression.
            // In Orchestrator:
            // ne = ... (find renewed node)
            // ne.setState(DONE)
            // ...
            // ne.getChildren().addAll(nodeExecutions)
            
            // So we should update 'ne' reference to point to the renewed node.
            ne = renewedNe;
        }

        if (ne.getWorkflowNode().getType().equals(WorkflowNodeType.END)) {
            we.setStatus(WorkflowExecutionStatus.SUCCESS);
        }

        ne.getChildren().addAll(nodeExecutions);

        WorkflowNodeExecution finalNe = ne;
        // Loop handling: remove old execution if we are re-entering a node
        if (we.getWorkflow().getNodes().stream().anyMatch(n -> n.getWorkflowNodeId().equals(finalNe.getWorkflowNodeId()))) {
             // Wait, this check: we.getWorkflow().getNodes()... checks if the node exists in the workflow definition.
             // That's always true for a valid node.
             // The logic seems to be: if this node is part of the workflow (which it is),
             // remove any existing execution for this node ID from the execution list?
             // No, 'we.getNodes().removeIf(...)' removes by *ExecutionId*.
             // But 'finalNe' has a new ExecutionId if it was just created?
             // If it's a loop, 'nodeExecutions' contains new executions.
             // When we process a child, we add it to 'we.getNodes()'.
             
             // The logic in Orchestrator:
             // if (we.getWorkflow().getNodes().stream().anyMatch(n -> n.getWorkflowNodeId().equals(finalNe.getWorkflowNodeId()))) {
             //    we.getNodes().removeIf(cne -> cne.getWorkflowNodeExecutionId().equals(finalNe.getWorkflowNodeExecutionId()));
             //    we.getNodes().add(ne);
             // }
             
             // This seems to be ensuring that 'ne' is in 'we.getNodes()', replacing any duplicate?
             // But 'ne' is the *current* node that just finished.
             // Why would we remove it and add it again?
             // Maybe to update its state in the list?
             
             we.getNodes().removeIf(cne -> cne.getWorkflowNodeExecutionId().equals(finalNe.getWorkflowNodeExecutionId()));
             we.getNodes().add(ne);
        }
        
        return nodeExecutions;
    }
}
