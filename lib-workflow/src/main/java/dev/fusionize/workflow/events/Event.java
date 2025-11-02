package dev.fusionize.workflow.events;

import dev.fusionize.common.utility.KeyUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.Objects;

@Document(collection = "workflow-event")
public abstract class Event {
    @Id
    private String id;
    @Indexed(unique = true)
    private String eventId;
    @Indexed()
    private String correlationId;
    @Indexed()
    private String causationId;
    private String eventClass;
    private ZonedDateTime generatedDate;
    private ZonedDateTime processedDate;
    @Transient
    protected transient Object source;

    protected Event renew() {
        try {
            Event newEvent = this.getClass()
                    .getDeclaredConstructor()
                    .newInstance();

            newEvent.setSource(this.source);
            newEvent.setCorrelationId(this.correlationId);
            newEvent.setCausationId(this.causationId);
            newEvent.setEventClass(this.eventClass);
            newEvent.setProcessedDate(null);
            newEvent.setGeneratedDate(ZonedDateTime.now());
            newEvent.setEventId(Builder.generateEventId());
            return newEvent;
        } catch (Exception ignore) {
            return null;
        }
    }

    public abstract static class Builder<T extends Builder<T>> {
        protected final String eventClass;
        protected final Object source;
        protected String eventId;
        protected String correlationId;
        protected String causationId;
        protected ZonedDateTime generatedDate;
        protected ZonedDateTime processedDate;

        protected Builder(Class<?> eventClass, Object source) {
            this.eventClass = eventClass.getCanonicalName();
            this.source = source;
        }

        public T eventId(String eventId) {
            this.eventId = eventId;
            return self() ;
        }

        public T correlationId(String correlationId) {
            this.correlationId = correlationId;
            return self();
        }

        public T causationId(String causationId) {
            this.causationId = causationId;
            return self();
        }

        public T generatedAt(ZonedDateTime dateTime) {
            this.generatedDate = dateTime;
            return self();
        }

        public T processedAt(ZonedDateTime dateTime) {
            this.processedDate = dateTime;
            return self();
        }

        private static String generateEventId(){
            return KeyUtil.getTimestampId("EVNT");
        }

        protected void load(Event event) {
            if (source == null) {
                throw new IllegalStateException("Source must be provided for an Event");
            }
            event.setSource(source);
            event.setEventId(Objects.requireNonNullElseGet(eventId, Builder::generateEventId));
            event.setCorrelationId(Objects.requireNonNullElseGet(correlationId, KeyUtil::getUUID));
            event.setCausationId(Objects.requireNonNullElseGet(causationId, KeyUtil::getUUID));
            event.setGeneratedDate(Objects.requireNonNullElseGet(generatedDate, ZonedDateTime::now));
            event.setEventClass(eventClass);
            event.setGeneratedDate(generatedDate);
            event.setProcessedDate(processedDate);
        }

        protected abstract T self();
        public abstract Event build();
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

    public String getEventClass() {
        return eventClass;
    }

    public void setEventClass(String eventClass) {
        this.eventClass = eventClass;
    }

    public ZonedDateTime getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(ZonedDateTime generatedDate) {
        this.generatedDate = generatedDate;
    }

    public ZonedDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(ZonedDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }
}
