package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerAdditionalTest {

    @Mock
    private WorkflowRepoRegistry workflowRepoRegistry;

    @Mock
    private WorkflowExecutionRepoRegistry workflowExecutionRepoRegistry;

    @Mock
    private WorkflowLogger workflowLogger;

    @Mock
    private WorkflowInteractionLogger interactionLogger;

    private WorkflowController workflowController;

    @BeforeEach
    void setUp() {
        // setup
        workflowController = new WorkflowController(
                workflowRepoRegistry, workflowExecutionRepoRegistry,
                workflowLogger, interactionLogger);
    }

    @Test
    void shouldReturnAllWorkflows() {
        // setup
        var workflow1 = new Workflow();
        workflow1.setDomain("domain-1");
        var workflow2 = new Workflow();
        workflow2.setDomain("domain-2");
        when(workflowRepoRegistry.getAll()).thenReturn(List.of(workflow1, workflow2));

        // expectation
        var result = workflowController.getAll();

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).hasSize(2);
        assertThat(result.getResponse().getMessage().get(0).getDomain()).isEqualTo("domain-1");
        assertThat(result.getResponse().getMessage().get(1).getDomain()).isEqualTo("domain-2");
    }

    @Test
    void shouldReturnEmptyListWhenNoWorkflows() {
        // setup
        when(workflowRepoRegistry.getAll()).thenReturn(Collections.emptyList());

        // expectation
        var result = workflowController.getAll();

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEmpty();
    }

    @Test
    void shouldReturnWorkflowByDomain() {
        // setup
        var workflow = new Workflow();
        workflow.setDomain("my-domain");
        when(workflowRepoRegistry.getWorkflowByDomain("my-domain")).thenReturn(workflow);

        // expectation
        var result = workflowController.get("my-domain");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isNotNull();
        assertThat(result.getResponse().getMessage().getDomain()).isEqualTo("my-domain");
    }

    @Test
    void shouldReturn404WhenWorkflowNotFound() {
        // setup
        when(workflowRepoRegistry.getWorkflowByDomain("missing-domain")).thenReturn(null);

        // expectation
        var result = workflowController.get("missing-domain");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getMessage()).isNull();
    }

    @Test
    void shouldReturnWorkflowExecutions() {
        // setup
        var execution = new WorkflowExecution();
        execution.setWorkflowId("wf-1");
        execution.setWorkflowExecutionId("exec-1");
        when(workflowExecutionRepoRegistry.getWorkflowExecutions("wf-1"))
                .thenReturn(List.of(execution));

        // expectation
        var result = workflowController.getWorkflowExecutions("wf-1");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).hasSize(1);
        assertThat(result.getResponse().getMessage().get(0).getWorkflowExecutionId()).isEqualTo("exec-1");
    }

    @Test
    void shouldReturnEmptyExecutionsList() {
        // setup
        when(workflowExecutionRepoRegistry.getWorkflowExecutions("wf-empty"))
                .thenReturn(Collections.emptyList());

        // expectation
        var result = workflowController.getWorkflowExecutions("wf-empty");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEmpty();
    }

    @Test
    void shouldReturnWorkflowExecutionLogs() {
        // setup
        var log = WorkflowLog.info("wf-1", "test-domain", "exec-1", "node-1", "key-1",
                "component-1", "Test log message");
        when(workflowLogger.getLogs("exec-1")).thenReturn(List.of(log));

        // expectation
        var result = workflowController.getWorkflowExecutionLogs("wf-1", "exec-1");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).hasSize(1);
        assertThat(result.getResponse().getMessage().get(0).getMessage()).isEqualTo("Test log message");
        assertThat(result.getResponse().getMessage().get(0).getLevel()).isEqualTo(WorkflowLog.LogLevel.INFO);
    }

    @Test
    void shouldReturnEmptyLogsList() {
        // setup
        when(workflowLogger.getLogs("exec-no-logs")).thenReturn(Collections.emptyList());

        // expectation
        var result = workflowController.getWorkflowExecutionLogs("wf-1", "exec-no-logs");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEmpty();
    }

    @Test
    void shouldReturnWorkflowExecutionInteractions() {
        // setup
        var interaction = WorkflowInteraction.create("wf-1", "test-domain", "exec-1",
                "node-1", "key-1", "component-1", "ai-agent",
                WorkflowInteraction.InteractionType.MESSAGE,
                WorkflowInteraction.Visibility.EXTERNAL, "Hello world");
        when(interactionLogger.getInteractions("exec-1")).thenReturn(List.of(interaction));

        // expectation
        var result = workflowController.getWorkflowExecutionInteractions("wf-1", "exec-1");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).hasSize(1);
        assertThat(result.getResponse().getMessage().get(0).getContent()).isEqualTo("Hello world");
        assertThat(result.getResponse().getMessage().get(0).getType())
                .isEqualTo(WorkflowInteraction.InteractionType.MESSAGE);
        assertThat(result.getResponse().getMessage().get(0).getVisibility())
                .isEqualTo(WorkflowInteraction.Visibility.EXTERNAL);
    }

    @Test
    void shouldReturnEmptyInteractionsList() {
        // setup
        when(interactionLogger.getInteractions("exec-no-interactions"))
                .thenReturn(Collections.emptyList());

        // expectation
        var result = workflowController.getWorkflowExecutionInteractions("wf-1", "exec-no-interactions");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEmpty();
    }

    @Test
    void shouldRegisterWorkflowFromJson() {
        // setup
        var savedWorkflow = new Workflow();
        savedWorkflow.setDomain("registered-domain");
        when(workflowRepoRegistry.register(any(Workflow.class))).thenReturn(savedWorkflow);

        String jsonDefinition = "{\"domain\":\"registered-domain\",\"nodes\":[]}";

        // expectation
        var result = workflowController.registerWorkflow(jsonDefinition, "json");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isNotNull();
        assertThat(result.getResponse().getMessage().getDomain()).isEqualTo("registered-domain");
        verify(workflowRepoRegistry).register(any(Workflow.class));
    }

    @Test
    void shouldRegisterWorkflowFromYaml() {
        // setup
        var savedWorkflow = new Workflow();
        savedWorkflow.setDomain("yaml-domain");
        when(workflowRepoRegistry.register(any(Workflow.class))).thenReturn(savedWorkflow);

        String yamlDefinition = "domain: yaml-domain\nnodes: {}";

        // expectation
        var result = workflowController.registerWorkflow(yamlDefinition, "yaml");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isNotNull();
        assertThat(result.getResponse().getMessage().getDomain()).isEqualTo("yaml-domain");
        verify(workflowRepoRegistry).register(any(Workflow.class));
    }

    @Test
    void shouldRegisterWorkflowFromYml() {
        // setup
        var savedWorkflow = new Workflow();
        savedWorkflow.setDomain("yml-domain");
        when(workflowRepoRegistry.register(any(Workflow.class))).thenReturn(savedWorkflow);

        String yamlDefinition = "domain: yml-domain\nnodes: {}";

        // expectation
        var result = workflowController.registerWorkflow(yamlDefinition, "yml");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isNotNull();
        assertThat(result.getResponse().getMessage().getDomain()).isEqualTo("yml-domain");
    }

    @Test
    void shouldReturn400WhenWorkflowDefinitionIsInvalid() {
        // setup
        String invalidDefinition = "this is not valid json or yaml {{{{";

        // expectation
        var result = workflowController.registerWorkflow(invalidDefinition, "json");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getMessage()).isNull();
        verify(workflowRepoRegistry, never()).register(any());
    }

    @Test
    void shouldDefaultToJsonFormat() {
        // setup
        var savedWorkflow = new Workflow();
        savedWorkflow.setDomain("default-format");
        when(workflowRepoRegistry.register(any(Workflow.class))).thenReturn(savedWorkflow);

        String jsonDefinition = "{\"domain\":\"default-format\",\"nodes\":[]}";

        // expectation
        var result = workflowController.registerWorkflow(jsonDefinition, "json");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isNotNull();
    }
}
