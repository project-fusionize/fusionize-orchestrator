package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class EndResponseEvent extends OrchestrationEvent {
    public EndResponseEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(EndResponseEvent.class, source);
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
        public EndResponseEvent build() {
            EndResponseEvent event = new EndResponseEvent(source);
            super.load(event);
            return event;
        }
    }

}
