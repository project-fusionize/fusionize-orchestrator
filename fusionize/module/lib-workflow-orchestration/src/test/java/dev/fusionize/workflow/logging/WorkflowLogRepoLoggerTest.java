package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.listeners.LogListener;
import dev.fusionize.workflow.WorkflowLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

class WorkflowLogRepoLoggerTest {

    private WorkflowLogRepository repository;
    private LogListener listener;
    private WorkflowLogRepoLogger logger;

    @BeforeEach
    void setUp() {
        repository = mock(WorkflowLogRepository.class);
        listener = mock(LogListener.class);
        // Pass the listener in the constructor to simulate auto-wiring
        logger = new WorkflowLogRepoLogger(repository, Collections.singletonList(listener));
    }

    @Test
    void logCheckListener() {
        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", WorkflowLog.LogLevel.INFO, "test message");

        // Verify repository save called
        verify(repository, timeout(1000)).save(any(WorkflowLog.class));

        // Verify listener called
        ArgumentCaptor<WorkflowLog> logCaptor = ArgumentCaptor.forClass(WorkflowLog.class);
        verify(listener, timeout(1000)).onLog(logCaptor.capture());

        WorkflowLog capturedLog = logCaptor.getValue();
        assertEquals("wf1", capturedLog.getWorkflowId());
        assertEquals("test message", capturedLog.getMessage());
    }

    @Test
    void dynamicListener() {
        LogListener dynamicListener = mock(LogListener.class);
        logger.addListener(dynamicListener);

        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", WorkflowLog.LogLevel.INFO, "dynamic test");

        verify(dynamicListener, timeout(1000)).onLog(any(WorkflowLog.class));

        logger.removeListener(dynamicListener);
        logger.log("wf1", "domain1", "exec1", "node1", "key1", "comp1", WorkflowLog.LogLevel.INFO, "dynamic test 2");

        // Should not be called again (only once from previous call)
        verify(dynamicListener, Mockito.times(1)).onLog(any(WorkflowLog.class));
    }
}
