package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionRepoRegistryTest {

    @Mock
    private WorkflowExecutionRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private WorkflowExecutionRepoRegistry registry;

    @Test
    void shouldGetWorkflowExecutions() {
        // setup
        var execution = spy(new WorkflowExecution());
        execution.setWorkflowExecutionId("exec-1");
        when(repository.findByWorkflowIdIn(List.of("wf-1"))).thenReturn(List.of(execution));

        // expectation
        List<WorkflowExecution> result = registry.getWorkflowExecutions("wf-1");

        // validation
        assertThat(result).hasSize(1);
        verify(execution).inflate();
        verify(repository).findByWorkflowIdIn(List.of("wf-1"));
    }

    @Test
    void shouldGetWorkflowExecution_whenFound() {
        // setup
        var execution = spy(new WorkflowExecution());
        execution.setWorkflowExecutionId("exec-1");
        when(repository.findByWorkflowExecutionId("exec-1")).thenReturn(Optional.of(execution));

        // expectation
        WorkflowExecution result = registry.getWorkflowExecution("exec-1");

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getWorkflowExecutionId()).isEqualTo("exec-1");
        verify(execution).inflate();
    }

    @Test
    void shouldReturnNull_whenWorkflowExecutionNotFound() {
        // setup
        when(repository.findByWorkflowExecutionId("missing-id")).thenReturn(Optional.empty());

        // expectation
        WorkflowExecution result = registry.getWorkflowExecution("missing-id");

        // validation
        assertThat(result).isNull();
    }

    @Test
    void shouldRegisterWorkflowExecution() {
        // setup
        var execution = spy(new WorkflowExecution());
        execution.setCreatedDate(Instant.now());
        when(repository.save(execution)).thenReturn(execution);

        // expectation
        WorkflowExecution result = registry.register(execution);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedDate()).isNotNull();
        verify(execution).flatten();
        verify(repository).save(execution);
    }

    @Test
    void shouldSetCreatedDate_whenNull_onRegister() {
        // setup
        var execution = spy(new WorkflowExecution());
        execution.setCreatedDate(null);
        when(repository.save(execution)).thenReturn(execution);

        // expectation
        registry.register(execution);

        // validation
        assertThat(execution.getCreatedDate()).isNotNull();
        assertThat(execution.getUpdatedDate()).isNotNull();
    }

    @Test
    void shouldUpdateNodeExecution() {
        // setup
        var nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNodeExecutionId("node-exec-1");
        nodeExecution.setCreatedDate(Instant.now());

        // expectation
        registry.updateNodeExecution("exec-1", nodeExecution);

        // validation
        assertThat(nodeExecution.getUpdatedDate()).isNotNull();
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(WorkflowExecution.class));
    }

    @Test
    void shouldSetCreatedDate_whenNull_onUpdateNodeExecution() {
        // setup
        var nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowNodeExecutionId("node-exec-1");
        nodeExecution.setCreatedDate(null);

        // expectation
        registry.updateNodeExecution("exec-1", nodeExecution);

        // validation
        assertThat(nodeExecution.getCreatedDate()).isNotNull();
        assertThat(nodeExecution.getUpdatedDate()).isNotNull();
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(WorkflowExecution.class));
    }

    @Test
    void shouldUpdateStatus() {
        // setup
        var status = WorkflowExecutionStatus.IN_PROGRESS;

        // expectation
        registry.updateStatus("exec-1", status);

        // validation
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(WorkflowExecution.class));
    }

    @Test
    void shouldDeleteIdlesForWorkflow() {
        // setup
        var workflowId = "wf-1";

        // expectation
        registry.deleteIdlesFor(workflowId);

        // validation
        verify(repository).deleteByWorkflowIdAndStatus(workflowId, WorkflowExecutionStatus.IDLE.getName());
    }
}
