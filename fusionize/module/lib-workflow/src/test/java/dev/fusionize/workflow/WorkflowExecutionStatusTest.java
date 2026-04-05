package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutionStatusTest {

    @Test
    void shouldGetByName() {
        // setup
        var idle = "IDLE";
        var inProgress = "IN_PROGRESS";
        var error = "ERROR";
        var success = "SUCCESS";
        var terminated = "TERMINATED";

        // expectation
        var idleStatus = WorkflowExecutionStatus.get(idle);
        var inProgressStatus = WorkflowExecutionStatus.get(inProgress);
        var errorStatus = WorkflowExecutionStatus.get(error);
        var successStatus = WorkflowExecutionStatus.get(success);
        var terminatedStatus = WorkflowExecutionStatus.get(terminated);

        // validation
        assertThat(idleStatus).isEqualTo(WorkflowExecutionStatus.IDLE);
        assertThat(inProgressStatus).isEqualTo(WorkflowExecutionStatus.IN_PROGRESS);
        assertThat(errorStatus).isEqualTo(WorkflowExecutionStatus.ERROR);
        assertThat(successStatus).isEqualTo(WorkflowExecutionStatus.SUCCESS);
        assertThat(terminatedStatus).isEqualTo(WorkflowExecutionStatus.TERMINATED);
    }

    @Test
    void shouldReturnNullForUnknownName() {
        // setup
        var unknownName = "UNKNOWN_STATUS";

        // expectation
        var result = WorkflowExecutionStatus.get(unknownName);

        // validation
        assertThat(result).isNull();
    }
}
