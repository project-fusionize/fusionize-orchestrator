package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class StartRequestEvent extends OrchestrationEvent {
    public StartRequestEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(StartRequestEvent.class, source);
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
        public StartRequestEvent build() {
            StartRequestEvent event = new StartRequestEvent(source);
            super.load(event);
            return event;
        }
    }

}