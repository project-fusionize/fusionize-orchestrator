package dev.fusionize.workflow.events.runtime;
import dev.fusionize.workflow.events.RuntimeEvent;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow-event")
public class ComponentTriggeredEvent extends RuntimeEvent {

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
            ComponentTriggeredEvent event = new ComponentTriggeredEvent();
            super.load(event);
            return event;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
