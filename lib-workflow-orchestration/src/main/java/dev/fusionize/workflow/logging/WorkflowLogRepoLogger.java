package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.WorkflowLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class WorkflowLogRepoLogger implements WorkflowLogger {
    private final WorkflowLogRepository repository;

    public WorkflowLogRepoLogger(WorkflowLogRepository repository) {
        this.repository = repository;
    }

    public void log(String workflowId, String workflowExecutionId, String workflowNodeId, String component,
            WorkflowLog.LogLevel level, String message) {
        WorkflowLog log = WorkflowLog.create(workflowId, workflowExecutionId, workflowNodeId, component, level, message);
        Logger logger = LoggerFactory.getLogger(component);
        switch (level) {
            case INFO -> logger.info(message);
            case WARN -> logger.warn(message);
            case ERROR -> logger.error(message);
            case DEBUG -> logger.debug(message);
        }
        CompletableFuture.runAsync(() -> repository.save(log));
    }

    public List<WorkflowLog> getLogs(String workflowExecutionId) {
        return repository.findByWorkflowExecutionIdOrderByTimestampAsc(workflowExecutionId);
    }
}
