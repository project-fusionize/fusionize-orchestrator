package dev.fusionize.workflow.component.event;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.Map;

public class RuntimeEvent<G extends RuntimeEventData> extends ApplicationEvent {
    private String eventId;
    private String correlationId;
    private String workflowId;
    private String workflowExecutionId;
    private String workflowNodeId;
    private String workflowNodeExecutionId;
    private String component;
    private Variant variant;
    private boolean processed;
    private G data;

    //todo remove
    @Transient
    public WorkflowExecution we;
    @Transient
    public WorkflowNodeExecution ne;

    public RuntimeEvent(Object source) {
        super(source);
    }

    public enum Variant {
        REQUEST("REQUEST"),
        RESPONSE("RESPONSE");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        private static final Map<String, Variant> lookup = new HashMap<>();

        static{
            for(Variant variant : Variant.values())
                lookup.put(variant.getName(), variant);
        }
        public static Variant get(String name){
            return lookup.get(name);
        }
    }

    // Private constructor for builder use
    private RuntimeEvent(Builder<G> builder) {
        super(builder.source);
        if(builder.eventId != null){
            this.eventId = builder.eventId;
        } else {
            this.eventId = KeyUtil.getTimestampId("REVN");
        }
        if(builder.correlationId != null){
            this.correlationId = builder.correlationId;
        } else {
            this.correlationId = KeyUtil.getUUID();
        }
        this.workflowId = builder.workflowId;
        this.workflowExecutionId = builder.workflowExecutionId;
        this.workflowNodeId = builder.workflowNodeId;
        this.workflowNodeExecutionId = builder.workflowNodeExecutionId;
        this.component = builder.component;
        this.processed = builder.processed;
        this.variant = builder.variant;
        this.data = builder.data;
    }

    public static class Builder<G extends RuntimeEventData> {
        private final Object source;
        private String eventId;
        private String correlationId;
        private String workflowId;
        private String workflowExecutionId;
        private String workflowNodeId;
        private String workflowNodeExecutionId;
        private String component;
        private Variant variant;
        private boolean processed;
        private G data;

        public Builder(Object source) {
            this.source = source;
        }


        public Builder<G> eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder<G> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<G> workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder<G> workflowExecutionId(String workflowExecutionId) {
            this.workflowExecutionId = workflowExecutionId;
            return this;
        }

        public Builder<G> workflowNodeId(String workflowNodeId) {
            this.workflowNodeId = workflowNodeId;
            return this;
        }

        public Builder<G> workflowNodeExecutionId(String workflowNodeExecutionId) {
            this.workflowNodeExecutionId = workflowNodeExecutionId;
            return this;
        }

        public Builder<G> component(String component) {
            this.component = component;
            return this;
        }

        public Builder<G> processed(boolean processed) {
            this.processed = processed;
            return this;
        }

        public Builder<G> variant(Variant variant) {
            this.variant = variant;
            return this;
        }

        public Builder<G> data(G data) {
            this.data = data;
            return this;
        }

        public RuntimeEvent<G> build() {
            if (source == null) {
                throw new IllegalArgumentException("Source cannot be null");
            }
            return new RuntimeEvent<>(this);
        }
    }

    private RuntimeEvent.Builder<G> from(Object source, RuntimeEvent<?> sourceEvent) {
        return new RuntimeEvent.Builder<G>(source)
                .correlationId(sourceEvent.getCorrelationId())
                .workflowExecutionId(sourceEvent.getWorkflowExecutionId())
                .workflowId(sourceEvent.getWorkflowId())
                .workflowNodeId(sourceEvent.getWorkflowNodeId())
                .workflowNodeExecutionId(sourceEvent.getWorkflowNodeExecutionId())
                .component(sourceEvent.getComponent());
    }

    public RuntimeEvent<G> fromSource(
            Object source,
            RuntimeEvent<?> sourceEvent,
            Variant variant,
            G data
    ) {
        RuntimeEvent<G> created = from(source, sourceEvent)
                .data(data)
                .build();
        created.variant = variant;
        created.we = sourceEvent.we;
        created.ne = sourceEvent.ne;
        return created;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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

    public String getWorkflowNodeExecutionId() {
        return workflowNodeExecutionId;
    }

    public void setWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        this.workflowNodeExecutionId = workflowNodeExecutionId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public G getData() {
        return data;
    }

    public void setData(G data) {
        this.data = data;
    }
}
