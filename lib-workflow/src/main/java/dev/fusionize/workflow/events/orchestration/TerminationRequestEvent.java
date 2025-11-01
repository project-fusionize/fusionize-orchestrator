package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class TerminationRequestEvent extends OrchestrationEvent {
    public TerminationRequestEvent(Object source) {
        super(source);
    }

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
            TerminationRequestEvent event = new TerminationRequestEvent(source);
            super.load(event);
            return event;
        }
    }

}
