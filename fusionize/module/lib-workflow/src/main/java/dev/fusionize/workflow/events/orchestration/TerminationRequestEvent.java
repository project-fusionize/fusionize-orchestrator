package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow-event")
public class TerminationRequestEvent extends OrchestrationEvent {

    public static Builder builder(Object source) {
        return new Builder(TerminationRequestEvent.class, source);
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
        public TerminationRequestEvent build() {
            TerminationRequestEvent event = new TerminationRequestEvent();
            super.load(event);
            return event;
        }
    }

}
