package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class ActivateRequestEvent extends OrchestrationEvent {
    public ActivateRequestEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(ActivateRequestEvent.class, source);
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
        public ActivateRequestEvent build() {
            ActivateRequestEvent event = new ActivateRequestEvent(source);
            super.load(event);
            return event;
        }
    }
}
