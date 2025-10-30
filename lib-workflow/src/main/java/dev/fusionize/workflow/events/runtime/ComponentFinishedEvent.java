package dev.fusionize.workflow.events.runtime;

import dev.fusionize.workflow.events.RuntimeEvent;

public class ComponentFinishedEvent extends RuntimeEvent {
    public ComponentFinishedEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(ComponentFinishedEvent.class, source);
    }

    @Override
    public ComponentFinishedEvent renew() {
        return (ComponentFinishedEvent) super.renew();
    }

    public static class Builder extends RuntimeEvent.Builder<Builder> {

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public ComponentFinishedEvent build() {
            ComponentFinishedEvent event = new ComponentFinishedEvent(source);
            super.load(event);
            return event;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
