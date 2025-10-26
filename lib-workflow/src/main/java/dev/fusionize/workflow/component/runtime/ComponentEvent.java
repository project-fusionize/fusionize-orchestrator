package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.event.RuntimeEventData;
import org.springframework.context.ApplicationEvent;

public class ComponentEvent<G extends RuntimeEventData>  extends ApplicationEvent {
    private String eventId;
    private String correlationId;
    private String causationId;
    private String component;
    private boolean processed;
    private G data;

    public ComponentEvent(Object source) {
        super(source);
    }

    public static class Builder<G extends RuntimeEventData> {
        private Object source;
        private String eventId;
        private String correlationId;
        private String causationId;
        private String component;
        private boolean processed;
        private G data;

        public Builder<G> source(Object source) {
            this.source = source;
            return this;
        }

        public Builder<G> eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder<G> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<G> causationId(String causationId) {
            this.causationId = causationId;
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

        public Builder<G> data(G data) {
            this.data = data;
            return this;
        }

        public ComponentEvent<G> build() {
            if (source == null) {
                throw new IllegalStateException("Source must be provided for ComponentEvent");
            }
            ComponentEvent<G> event = new ComponentEvent<>(source);
            event.setEventId(eventId);
            event.setCorrelationId(correlationId);
            event.setCausationId(causationId);
            event.setComponent(component);
            event.setProcessed(processed);
            event.setData(data);
            return event;
        }
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

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
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

    public G getData() {
        return data;
    }

    public void setData(G data) {
        this.data = data;
    }
}
