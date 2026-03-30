package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.listeners.InteractionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowInteractionRepoLoggerTest {

    @Mock
    private WorkflowInteractionRepository repository;

    private WorkflowInteractionRepoLogger logger;

    @BeforeEach
    void setUp() {
        logger = new WorkflowInteractionRepoLogger(repository, new ArrayList<>());
    }

    @Test
    void shouldInitializeWithListeners() {
        // setup
        var listener = mock(InteractionListener.class);
        var listeners = List.of(listener);

        // expectation
        var loggerWithListeners = new WorkflowInteractionRepoLogger(repository, listeners);

        // validation
        loggerWithListeners.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", "actor1",
                WorkflowInteraction.InteractionType.MESSAGE, WorkflowInteraction.Visibility.EXTERNAL, "content");
        verify(listener, timeout(1000)).onInteraction(any(WorkflowInteraction.class));
    }

    @Test
    void shouldInitializeWithNullListeners() {
        // setup
        // pass null as listeners list

        // expectation
        assertThatNoException().isThrownBy(() -> new WorkflowInteractionRepoLogger(repository, null));

        // validation
        var loggerWithNull = new WorkflowInteractionRepoLogger(repository, null);
        assertThat(loggerWithNull).isNotNull();
    }

    @Test
    void shouldAddListener() {
        // setup
        var listener = mock(InteractionListener.class);

        // expectation
        logger.addListener(listener);

        // validation
        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", "actor1",
                WorkflowInteraction.InteractionType.MESSAGE, WorkflowInteraction.Visibility.EXTERNAL, "content");
        verify(listener, timeout(1000)).onInteraction(any(WorkflowInteraction.class));
    }

    @Test
    void shouldRemoveListener() {
        // setup
        var listener = mock(InteractionListener.class);
        logger.addListener(listener);

        // expectation
        logger.removeListener(listener);

        // validation
        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", "actor1",
                WorkflowInteraction.InteractionType.MESSAGE, WorkflowInteraction.Visibility.EXTERNAL, "content");
        verify(repository, timeout(1000)).save(any(WorkflowInteraction.class));
        verify(listener, never()).onInteraction(any(WorkflowInteraction.class));
    }

    @Test
    void shouldLogInteraction_asynchronously() {
        // setup
        var workflowId = "wf1";
        var domain = "domain1";
        var executionId = "exec1";
        var nodeId = "node1";
        var nodeKey = "key1";
        var component = "comp1";
        var actor = "actor1";
        var type = WorkflowInteraction.InteractionType.THOUGHT;
        var visibility = WorkflowInteraction.Visibility.INTERNAL;
        var content = "test content";

        // expectation
        logger.log(workflowId, domain, executionId, nodeId, nodeKey, component, actor, type, visibility, content);

        // validation
        verify(repository, timeout(1000)).save(any(WorkflowInteraction.class));
    }

    @Test
    void shouldNotifyListeners_onLog() {
        // setup
        var listener = mock(InteractionListener.class);
        logger.addListener(listener);

        // expectation
        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", "actor1",
                WorkflowInteraction.InteractionType.OBSERVATION, WorkflowInteraction.Visibility.EXTERNAL, "observed");

        // validation
        verify(listener, timeout(1000)).onInteraction(any(WorkflowInteraction.class));
    }

    @Test
    void shouldGetInteractions() {
        // setup
        var executionId = "exec1";
        var interaction = WorkflowInteraction.create("wf1", "domain1", executionId, "node1", "key1",
                "comp1", "actor1", WorkflowInteraction.InteractionType.MESSAGE,
                WorkflowInteraction.Visibility.EXTERNAL, "content");
        var expectedList = List.of(interaction);
        when(repository.findByWorkflowExecutionIdOrderByTimestampAsc(executionId)).thenReturn(expectedList);

        // expectation
        var result = logger.getInteractions(executionId);

        // validation
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedList);
        verify(repository).findByWorkflowExecutionIdOrderByTimestampAsc(executionId);
    }
}
