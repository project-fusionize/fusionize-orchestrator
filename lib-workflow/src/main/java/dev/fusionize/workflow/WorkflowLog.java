package dev.fusionize.workflow;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "workflow-log")
public class WorkflowLog {
    @Id
    private String id;
    @Indexed
    private String workflowId;
    @Indexed
    private String workflowExecutionId;
    @Indexed
    private String workflowNodeId;
    private String component;
    private Instant timestamp;
    private LogLevel level;
    private String message;
    private Map<String, Object> context;

    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }

    public static WorkflowLog info(String workflowId, String workflowExecutionId, String workflowNodeId,
            String component, String message) {
        return create(workflowId, workflowExecutionId, workflowNodeId, component, LogLevel.INFO, message);
    }

    public static WorkflowLog error(String workflowId, String workflowExecutionId, String workflowNodeId,
            String component, String message) {
        return create(workflowId, workflowExecutionId, workflowNodeId, component, LogLevel.ERROR, message);
    }

    public static WorkflowLog create(String workflowId, String workflowExecutionId, String workflowNodeId,
            String component, LogLevel level, String message) {
        WorkflowLog log = new WorkflowLog();
        log.workflowId = workflowId;
        log.workflowExecutionId = workflowExecutionId;
        log.workflowNodeId = workflowNodeId;
        log.component = component;
        log.timestamp = Instant.now();
        log.level = level;
        log.message = message;
        return log;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(String workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public String toString() {
        String str = String.format("%s %s [%s] [%s] [%s] %s: %s",
                timestamp, level, workflowId, workflowNodeId, workflowExecutionId, component, message);
        return context != null ? str + ": " + context : str;
    }
}
