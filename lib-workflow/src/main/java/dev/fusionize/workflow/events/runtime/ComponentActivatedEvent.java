package dev.fusionize.workflow.events.runtime;
import dev.fusionize.workflow.events.RuntimeEvent;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow-event")
public class ComponentActivatedEvent extends RuntimeEvent {

    public static Builder builder(Object source) {
        return new Builder(ComponentActivatedEvent.class, source);
    }

    @Override
    public ComponentActivatedEvent renew() {
        return (ComponentActivatedEvent) super.renew();
    }

    public static class Builder extends RuntimeEvent.Builder<Builder> {
        private Boolean activated;

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public ComponentActivatedEvent build() {
            ComponentActivatedEvent event = new ComponentActivatedEvent();
            super.load(event);
            return event;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
