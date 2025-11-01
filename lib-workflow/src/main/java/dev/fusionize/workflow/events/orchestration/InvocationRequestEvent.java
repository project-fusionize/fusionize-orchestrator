package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class InvocationRequestEvent extends OrchestrationEvent {
    public InvocationRequestEvent(Object source) {
        super(source);
    }

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
            InvocationRequestEvent event = new InvocationRequestEvent(source);
            super.load(event);
            return event;
        }
    }

}