package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.WorkflowNodeExecutionState;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.context.ContextFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiConsumer;

@Component
public class OrchestratorWorkflowNavigator {

    private final OrchestratorDecisionEngine decisionEngine;

    public OrchestratorWorkflowNavigator(OrchestratorDecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    public void navigate(WorkflowExecution we,
                         WorkflowNodeExecution ne,
                         BiConsumer<WorkflowExecution, WorkflowNodeExecution> next) {
        WorkflowNodeExecution originalExecutionNode = ne;
        ne.setState(WorkflowNodeExecutionState.DONE);
        List<WorkflowNodeExecution> nodeExecutions = decisionEngine.determineNextNodes(ne).stream()
                .map(n -> WorkflowNodeExecution.of(n, ContextFactory.from(originalExecutionNode, n)))
                .toList();

        if (ne.getWorkflowNode().getType().equals(WorkflowNodeType.START)) {
            we = we.renew();
            we.setStatus(WorkflowExecutionStatus.IN_PROGRESS);

            // Find the existing execution for the start node in the renewed list, or use the renewed one
            // So we need to find the corresponding NEW execution in 'we'.
            
            WorkflowNodeExecution renewedNe = we.getNodes().stream()
                    .filter(n -> n.getWorkflowNodeId().equals(originalExecutionNode.getWorkflowNodeId()))
                    .findFirst()
                    .orElse(ne.renew()); // Fallback, though it should be there if renew() works correctly
            
            renewedNe.setState(WorkflowNodeExecutionState.DONE);
            
            // We need to return the *new* children executions, but linked to the *new* parent
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
             we.getNodes().removeIf(cne -> cne.getWorkflowNodeExecutionId().equals(finalNe.getWorkflowNodeExecutionId()));
             we.getNodes().add(ne);
        }

        next.accept(we, ne);
    }
}
