package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowOrchestrationControllerTest {

    @Mock
    private Orchestrator orchestrator;

    @InjectMocks
    private WorkflowOrchestrationController workflowOrchestrationController;

    @Test
    void shouldReplayExecution() {
        // setup
        var workflowId = "wf-1";
        var workflowExecutionId = "exec-1";
        var workflowNodeExecutionId = "node-exec-1";

        // expectation
        workflowOrchestrationController.replayWorkflowNodeExecution(
                workflowId, workflowExecutionId, workflowNodeExecutionId);

        // validation
        verify(orchestrator).replayExecution(workflowId, workflowExecutionId, workflowNodeExecutionId);
    }

    @Test
    void shouldReturnSuccessResponse() {
        // setup
        var workflowId = "wf-1";
        var workflowExecutionId = "exec-1";
        var workflowNodeExecutionId = "node-exec-1";

        // expectation
        var result = workflowOrchestrationController.replayWorkflowNodeExecution(
                workflowId, workflowExecutionId, workflowNodeExecutionId);

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEqualTo("Replayed");
    }
}
