package dev.fusionize.workflow;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "workflow-interaction")
public class WorkflowInteraction {
    @Id
    private String id;
    @Indexed
    private String workflowId;
    @Indexed
    private String workflowExecutionId;
    @Indexed
    private String workflowNodeId;
    @Indexed
    private String workflowDomain;
    @Indexed
    private String nodeKey;
    private String component;
    private Instant timestamp;
    private String actor;
    private InteractionType type;
    private Visibility visibility;
    private Object content;
    private Map<String, Object> context;

    public enum InteractionType {
        MESSAGE, THOUGHT, OBSERVATION
    }

    public enum Visibility {
        INTERNAL, EXTERNAL
    }

    public static WorkflowInteraction create(String workflowId, String workflowDomain, String workflowExecutionId,
                                           String workflowNodeId, String nodeKey, String component,
                                           String actor, InteractionType type, Visibility visibility, Object content) {
        WorkflowInteraction interaction = new WorkflowInteraction();
        interaction.workflowId = workflowId;
        interaction.workflowDomain = workflowDomain;
        interaction.workflowExecutionId = workflowExecutionId;
        interaction.workflowNodeId = workflowNodeId;
        interaction.nodeKey = nodeKey;
        interaction.component = component;
        interaction.timestamp = Instant.now();
        interaction.actor = actor;
        interaction.type = type;
        interaction.visibility = visibility;
        interaction.content = content;
        return interaction;
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

    public String getWorkflowDomain() {
        return workflowDomain;
    }

    public void setWorkflowDomain(String workflowDomain) {
        this.workflowDomain = workflowDomain;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
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

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public InteractionType getType() {
        return type;
    }

    public void setType(InteractionType type) {
        this.type = type;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public String toString() {
        String str = String.format("%s %-5s %-5s [%-25s] [%45s] %s: %s",
                timestamp, type, visibility, workflowExecutionId, workflowDomain + ":" + nodeKey, component + "(" + actor + ")",
                content);
        return context != null ? str + ": " + context : str;
    }
}
