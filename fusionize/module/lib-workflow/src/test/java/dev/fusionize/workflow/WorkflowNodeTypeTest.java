package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNodeTypeTest {

    @Test
    void shouldGetByName() {
        // setup
        var start = "START";
        var decision = "DECISION";
        var task = "TASK";
        var wait = "WAIT";
        var end = "END";

        // expectation
        var startType = WorkflowNodeType.get(start);
        var decisionType = WorkflowNodeType.get(decision);
        var taskType = WorkflowNodeType.get(task);
        var waitType = WorkflowNodeType.get(wait);
        var endType = WorkflowNodeType.get(end);

        // validation
        assertThat(startType).isEqualTo(WorkflowNodeType.START);
        assertThat(decisionType).isEqualTo(WorkflowNodeType.DECISION);
        assertThat(taskType).isEqualTo(WorkflowNodeType.TASK);
        assertThat(waitType).isEqualTo(WorkflowNodeType.WAIT);
        assertThat(endType).isEqualTo(WorkflowNodeType.END);
    }

    @Test
    void shouldReturnNullForUnknownName() {
        // setup
        var unknownName = "UNKNOWN_TYPE";

        // expectation
        var result = WorkflowNodeType.get(unknownName);

        // validation
        assertThat(result).isNull();
    }
}
