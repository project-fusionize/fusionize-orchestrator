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

    public void navigate(
            WorkflowExecution we,
            WorkflowNodeExecution ne,
            BiConsumer<WorkflowExecution, WorkflowNodeExecution> next
    ) {
        WorkflowNodeExecution current = ne;

        // 1. Handle START node special case
        if (isStartNode(current)) {
            we = startWorkflow(we, current);
            current = findRenewedExecution(we, current);
        } else {
            current.setState(WorkflowNodeExecutionState.DONE);
        }

        // 2. Mark workflow complete if END node
        if (isEndNode(current)) {
            we.setStatus(WorkflowExecutionStatus.SUCCESS);
        }

        // 3. Determine and attach next node executions
        List<WorkflowNodeExecution> children = createNextExecutions(current);
        current.getChildren().addAll(children);

        // 4. Handle loop re-entry (replace old execution)
        reconcileExecutionInWorkflow(we, current);

        // 5. Continue navigation
        next.accept(we, current);
    }

    private boolean isStartNode(WorkflowNodeExecution ne) {
        return ne.getWorkflowNode().getType() == WorkflowNodeType.START;
    }

    private boolean isEndNode(WorkflowNodeExecution ne) {
        return ne.getWorkflowNode().getType() == WorkflowNodeType.END;
    }

    private WorkflowExecution startWorkflow(WorkflowExecution we, WorkflowNodeExecution ne) {
        WorkflowExecution renewed = we.renew();
        renewed.setStatus(WorkflowExecutionStatus.IN_PROGRESS);
        return renewed;
    }

    private WorkflowNodeExecution findRenewedExecution(
            WorkflowExecution we,
            WorkflowNodeExecution original
    ) {
        WorkflowNodeExecution renewed = we.getNodes().stream()
                .filter(n -> n.getWorkflowNodeId().equals(original.getWorkflowNodeId()))
                .findFirst()
                .orElse(original.renew());

        renewed.setState(WorkflowNodeExecutionState.DONE);
        return renewed;
    }

    private List<WorkflowNodeExecution> createNextExecutions(WorkflowNodeExecution parent) {
        return decisionEngine.determineNextNodes(parent).stream()
                .map(n -> WorkflowNodeExecution.of(
                        n,
                        ContextFactory.from(parent, n)
                ))
                .toList();
    }

    private void reconcileExecutionInWorkflow(
            WorkflowExecution we,
            WorkflowNodeExecution ne
    ) {
        boolean exists = we.getWorkflow().getNodes().stream()
                .anyMatch(n -> n.getWorkflowNodeId().equals(ne.getWorkflowNodeId()));

        if (!exists) {
            return;
        }

        we.getNodes().removeIf(
                existing -> existing.getWorkflowNodeExecutionId()
                        .equals(ne.getWorkflowNodeExecutionId())
        );
        we.getNodes().add(ne);
    }
}
