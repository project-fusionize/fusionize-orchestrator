package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class EndRequestEvent extends OrchestrationEvent {
    public EndRequestEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(EndRequestEvent.class, source);
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
        public EndRequestEvent build() {
            EndRequestEvent event = new EndRequestEvent(source);
            super.load(event);
            return event;
        }
    }

}
