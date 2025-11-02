package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow-event")
public class InvocationRequestEvent extends OrchestrationEvent {

    public static Builder builder(Object source) {
        return new Builder(InvocationRequestEvent.class, source);
    }

    public static class Builder extends OrchestrationEvent.Builder<Builder> {

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public InvocationRequestEvent build() {
            InvocationRequestEvent event = new InvocationRequestEvent();
            super.load(event);
            return event;
        }
    }

}