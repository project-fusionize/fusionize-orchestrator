package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class ActivationRequestEvent extends OrchestrationEvent {
    public ActivationRequestEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(ActivationRequestEvent.class, source);
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
        public ActivationRequestEvent build() {
            ActivationRequestEvent event = new ActivationRequestEvent(source);
            super.load(event);
            return event;
        }
    }
}
