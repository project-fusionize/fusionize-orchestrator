package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeExecutionStateTest {

    @Test
    void shouldGetByName() {
        // setup
        var idle = "IDLE";
        var working = "WORKING";
        var waiting = "WAITING";
        var failed = "FAILED";
        var done = "DONE";

        // expectation
        var idleState = WorkflowNodeExecutionState.get(idle);
        var workingState = WorkflowNodeExecutionState.get(working);
        var waitingState = WorkflowNodeExecutionState.get(waiting);
        var failedState = WorkflowNodeExecutionState.get(failed);
        var doneState = WorkflowNodeExecutionState.get(done);

        // validation
        assertThat(idleState).isEqualTo(WorkflowNodeExecutionState.IDLE);
        assertThat(workingState).isEqualTo(WorkflowNodeExecutionState.WORKING);
        assertThat(waitingState).isEqualTo(WorkflowNodeExecutionState.WAITING);
        assertThat(failedState).isEqualTo(WorkflowNodeExecutionState.FAILED);
        assertThat(doneState).isEqualTo(WorkflowNodeExecutionState.DONE);
    }

    @Test
    void shouldReturnNullForUnknownName() {
        // setup
        var unknownName = "UNKNOWN_STATE";

        // expectation
        var result = WorkflowNodeExecutionState.get(unknownName);

        // validation
        assertThat(result).isNull();
    }
}
