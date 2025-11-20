package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.WorkflowLogger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowLogRepoLogger implements WorkflowLogger {
    private final WorkflowLogRepository repository;

    public WorkflowLogRepoLogger(WorkflowLogRepository repository) {
        this.repository = repository;
    }

    public void log(String workflowId, String workflowExecutionId, String workflowNodeId, String component,
            String message) {
        System.out.println("WorkflowLogger: Saving log: " + message);
        repository.save(WorkflowLog.info(workflowId, workflowExecutionId, workflowNodeId, component, message));
    }

    public void log(String workflowId, String workflowExecutionId, String workflowNodeId, String component,
            WorkflowLog.LogLevel level, String message) {
        repository.save(WorkflowLog.create(workflowId, workflowExecutionId, workflowNodeId, component, level, message));
    }

    public List<WorkflowLog> getLogs(String workflowExecutionId) {
        return repository.findByWorkflowExecutionIdOrderByTimestampAsc(workflowExecutionId);
    }
}
