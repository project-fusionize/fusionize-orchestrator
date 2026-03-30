package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowLogTest {

    @Test
    void shouldCreateInfoLog() {
        // setup
        var workflowId = "wf-1";
        var workflowDomain = "domain-1";
        var workflowExecutionId = "exec-1";
        var workflowNodeId = "node-1";
        var nodeKey = "key-1";
        var component = "comp-1";
        var message = "info message";

        // expectation
        var log = WorkflowLog.info(workflowId, workflowDomain, workflowExecutionId,
                workflowNodeId, nodeKey, component, message);

        // validation
        assertThat(log.getWorkflowId()).isEqualTo(workflowId);
        assertThat(log.getWorkflowDomain()).isEqualTo(workflowDomain);
        assertThat(log.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
        assertThat(log.getWorkflowNodeId()).isEqualTo(workflowNodeId);
        assertThat(log.getNodeKey()).isEqualTo(nodeKey);
        assertThat(log.getComponent()).isEqualTo(component);
        assertThat(log.getMessage()).isEqualTo(message);
        assertThat(log.getLevel()).isEqualTo(WorkflowLog.LogLevel.INFO);
        assertThat(log.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateErrorLog() {
        // setup
        var workflowId = "wf-2";
        var workflowDomain = "domain-2";
        var workflowExecutionId = "exec-2";
        var workflowNodeId = "node-2";
        var nodeKey = "key-2";
        var component = "comp-2";
        var message = "error message";

        // expectation
        var log = WorkflowLog.error(workflowId, workflowDomain, workflowExecutionId,
                workflowNodeId, nodeKey, component, message);

        // validation
        assertThat(log.getLevel()).isEqualTo(WorkflowLog.LogLevel.ERROR);
        assertThat(log.getMessage()).isEqualTo(message);
        assertThat(log.getWorkflowId()).isEqualTo(workflowId);
    }

    @Test
    void shouldSetAllFieldsViaCreate() {
        // setup
        var workflowId = "wf-3";
        var workflowDomain = "domain-3";
        var workflowExecutionId = "exec-3";
        var workflowNodeId = "node-3";
        var nodeKey = "key-3";
        var component = "comp-3";
        var level = WorkflowLog.LogLevel.WARN;
        var message = "warn message";

        // expectation
        var log = WorkflowLog.create(workflowId, workflowDomain, workflowExecutionId,
                workflowNodeId, nodeKey, component, level, message);

        // validation
        assertThat(log.getWorkflowId()).isEqualTo(workflowId);
        assertThat(log.getWorkflowDomain()).isEqualTo(workflowDomain);
        assertThat(log.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
        assertThat(log.getWorkflowNodeId()).isEqualTo(workflowNodeId);
        assertThat(log.getNodeKey()).isEqualTo(nodeKey);
        assertThat(log.getComponent()).isEqualTo(component);
        assertThat(log.getLevel()).isEqualTo(level);
        assertThat(log.getMessage()).isEqualTo(message);
        assertThat(log.getTimestamp()).isNotNull();
    }

    @Test
    void shouldReturnToStringWithContext() {
        // setup
        var log = WorkflowLog.info("wf-1", "domain-1", "exec-1", "node-1", "key-1", "comp-1", "msg");
        log.setContext(Map.of("detail", "value"));

        // expectation
        var result = log.toString();

        // validation
        assertThat(result).contains("comp-1");
        assertThat(result).contains("msg");
        assertThat(result).contains("detail");
        assertThat(result).contains("value");
    }

    @Test
    void shouldReturnToStringWithoutContext() {
        // setup
        var log = WorkflowLog.info("wf-1", "domain-1", "exec-1", "node-1", "key-1", "comp-1", "msg");

        // expectation
        var result = log.toString();

        // validation
        assertThat(result).contains("comp-1");
        assertThat(result).contains("msg");
        assertThat(log.getContext()).isNull();
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var log = new WorkflowLog();
        var id = "id-1";
        var workflowId = "wf-set";
        var workflowExecutionId = "exec-set";
        var workflowNodeId = "node-set";
        var workflowDomain = "domain-set";
        var nodeKey = "key-set";
        var component = "comp-set";
        var timestamp = Instant.now();
        var level = WorkflowLog.LogLevel.DEBUG;
        var message = "debug msg";
        var context = Map.<String, Object>of("k", "v");

        // expectation
        log.setId(id);
        log.setWorkflowId(workflowId);
        log.setWorkflowExecutionId(workflowExecutionId);
        log.setWorkflowNodeId(workflowNodeId);
        log.setWorkflowDomain(workflowDomain);
        log.setNodeKey(nodeKey);
        log.setComponent(component);
        log.setTimestamp(timestamp);
        log.setLevel(level);
        log.setMessage(message);
        log.setContext(context);

        // validation
        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getWorkflowId()).isEqualTo(workflowId);
        assertThat(log.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
        assertThat(log.getWorkflowNodeId()).isEqualTo(workflowNodeId);
        assertThat(log.getWorkflowDomain()).isEqualTo(workflowDomain);
        assertThat(log.getNodeKey()).isEqualTo(nodeKey);
        assertThat(log.getComponent()).isEqualTo(component);
        assertThat(log.getTimestamp()).isEqualTo(timestamp);
        assertThat(log.getLevel()).isEqualTo(level);
        assertThat(log.getMessage()).isEqualTo(message);
        assertThat(log.getContext()).isEqualTo(context);
    }
}
