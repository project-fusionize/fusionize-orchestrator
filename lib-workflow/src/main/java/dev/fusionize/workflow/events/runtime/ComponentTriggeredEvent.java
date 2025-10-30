package dev.fusionize.workflow.events.runtime;
import dev.fusionize.workflow.events.RuntimeEvent;

public class ComponentTriggeredEvent extends RuntimeEvent {
    public ComponentTriggeredEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(ComponentTriggeredEvent.class, source);
    }

    @Override
    public ComponentTriggeredEvent renew() {
        return (ComponentTriggeredEvent) super.renew();
    }

    public static class Builder extends RuntimeEvent.Builder<Builder> {

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public ComponentTriggeredEvent build() {
            ComponentTriggeredEvent event = new ComponentTriggeredEvent(source);
            super.load(event);
            return event;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
