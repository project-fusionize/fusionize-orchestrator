package dev.fusionize.workflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowInteractionTest {

    @Test
    void shouldCreateInteraction() {
        // setup
        var workflowId = "wf-1";
        var workflowDomain = "domain-1";
        var workflowExecutionId = "exec-1";
        var workflowNodeId = "node-1";
        var nodeKey = "key-1";
        var component = "comp-1";
        var actor = "actor-1";
        var type = WorkflowInteraction.InteractionType.MESSAGE;
        var visibility = WorkflowInteraction.Visibility.EXTERNAL;
        var content = "hello world";

        // expectation
        var interaction = WorkflowInteraction.create(workflowId, workflowDomain, workflowExecutionId,
                workflowNodeId, nodeKey, component, actor, type, visibility, content);

        // validation
        assertThat(interaction.getWorkflowId()).isEqualTo(workflowId);
        assertThat(interaction.getWorkflowDomain()).isEqualTo(workflowDomain);
        assertThat(interaction.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
        assertThat(interaction.getWorkflowNodeId()).isEqualTo(workflowNodeId);
        assertThat(interaction.getNodeKey()).isEqualTo(nodeKey);
        assertThat(interaction.getComponent()).isEqualTo(component);
        assertThat(interaction.getActor()).isEqualTo(actor);
        assertThat(interaction.getType()).isEqualTo(type);
        assertThat(interaction.getVisibility()).isEqualTo(visibility);
        assertThat(interaction.getContent()).isEqualTo(content);
        assertThat(interaction.getTimestamp()).isNotNull();
    }

    @Test
    void shouldSetAllFieldsViaCreate() {
        // setup
        var type = WorkflowInteraction.InteractionType.THOUGHT;
        var visibility = WorkflowInteraction.Visibility.INTERNAL;
        var content = Map.of("key", "value");

        // expectation
        var interaction = WorkflowInteraction.create("wf-2", "domain-2", "exec-2",
                "node-2", "key-2", "comp-2", "actor-2", type, visibility, content);

        // validation
        assertThat(interaction.getType()).isEqualTo(WorkflowInteraction.InteractionType.THOUGHT);
        assertThat(interaction.getVisibility()).isEqualTo(WorkflowInteraction.Visibility.INTERNAL);
        assertThat(interaction.getContent()).isEqualTo(content);
    }

    @Test
    void shouldReturnToStringWithContext() {
        // setup
        var interaction = WorkflowInteraction.create("wf-1", "domain-1", "exec-1",
                "node-1", "key-1", "comp-1", "actor-1",
                WorkflowInteraction.InteractionType.OBSERVATION,
                WorkflowInteraction.Visibility.EXTERNAL, "content");
        interaction.setContext(Map.of("detail", "val"));

        // expectation
        var result = interaction.toString();

        // validation
        assertThat(result).contains("comp-1");
        assertThat(result).contains("actor-1");
        assertThat(result).contains("content");
        assertThat(result).contains("detail");
    }

    @Test
    void shouldReturnToStringWithoutContext() {
        // setup
        var interaction = WorkflowInteraction.create("wf-1", "domain-1", "exec-1",
                "node-1", "key-1", "comp-1", "actor-1",
                WorkflowInteraction.InteractionType.MESSAGE,
                WorkflowInteraction.Visibility.INTERNAL, "content");

        // expectation
        var result = interaction.toString();

        // validation
        assertThat(result).contains("comp-1");
        assertThat(result).contains("content");
        assertThat(interaction.getContext()).isNull();
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var interaction = new WorkflowInteraction();
        var id = "id-1";
        var workflowId = "wf-set";
        var workflowExecutionId = "exec-set";
        var workflowNodeId = "node-set";
        var workflowDomain = "domain-set";
        var nodeKey = "key-set";
        var component = "comp-set";
        var timestamp = Instant.now();
        var actor = "actor-set";
        var type = WorkflowInteraction.InteractionType.OBSERVATION;
        var visibility = WorkflowInteraction.Visibility.EXTERNAL;
        var content = (Object) "some content";
        var context = Map.<String, Object>of("k", "v");

        // expectation
        interaction.setId(id);
        interaction.setWorkflowId(workflowId);
        interaction.setWorkflowExecutionId(workflowExecutionId);
        interaction.setWorkflowNodeId(workflowNodeId);
        interaction.setWorkflowDomain(workflowDomain);
        interaction.setNodeKey(nodeKey);
        interaction.setComponent(component);
        interaction.setTimestamp(timestamp);
        interaction.setActor(actor);
        interaction.setType(type);
        interaction.setVisibility(visibility);
        interaction.setContent(content);
        interaction.setContext(context);

        // validation
        assertThat(interaction.getId()).isEqualTo(id);
        assertThat(interaction.getWorkflowId()).isEqualTo(workflowId);
        assertThat(interaction.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
        assertThat(interaction.getWorkflowNodeId()).isEqualTo(workflowNodeId);
        assertThat(interaction.getWorkflowDomain()).isEqualTo(workflowDomain);
        assertThat(interaction.getNodeKey()).isEqualTo(nodeKey);
        assertThat(interaction.getComponent()).isEqualTo(component);
        assertThat(interaction.getTimestamp()).isEqualTo(timestamp);
        assertThat(interaction.getActor()).isEqualTo(actor);
        assertThat(interaction.getType()).isEqualTo(type);
        assertThat(interaction.getVisibility()).isEqualTo(visibility);
        assertThat(interaction.getContent()).isEqualTo(content);
        assertThat(interaction.getContext()).isEqualTo(context);
    }
}
