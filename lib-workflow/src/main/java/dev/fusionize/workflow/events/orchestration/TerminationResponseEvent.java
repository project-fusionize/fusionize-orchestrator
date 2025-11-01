package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class TerminationResponseEvent extends OrchestrationEvent {
    public TerminationResponseEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(TerminationResponseEvent.class, source);
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
        public TerminationResponseEvent build() {
            TerminationResponseEvent event = new TerminationResponseEvent(source);
            super.load(event);
            return event;
        }
    }

}
